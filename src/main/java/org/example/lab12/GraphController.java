package org.example.lab12;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;


public class GraphController {

    @FXML
    private TextField vertexCountField, startVertexField, endVertexField;
    @FXML
    private Label resultLabel;
    @FXML
    private Pane graphPane;

    private List<Vertex> vertices = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private Vertex selectedVertex = null;  // Выделенная вершина для добавления рёбер
    private int maxVertexCount = 0;  // Количество вершин для использования по умолчанию
    private double orgSceneX, orgSceneY;  // Для перемещения вершины

    private int[][] adjacencyMatrix = new int[maxVertexCount][maxVertexCount];  // Матрица смежности для хранения графа

    // Метод задания количества вершин
    public void setMaxVertexCount() {
        try {
            int newMaxVertexCount = Integer.parseInt(vertexCountField.getText());
            if (newMaxVertexCount <= 0 || newMaxVertexCount > 10) {
                throw new NumberFormatException();
            }

            // Если новая матрица должна быть больше, пересоздаем её
            if (newMaxVertexCount > maxVertexCount) {
                int[][] newAdjacencyMatrix = new int[newMaxVertexCount][newMaxVertexCount];

                // Копируем значения из старой матрицы в новую
                for (int i = 0; i < maxVertexCount; i++) {
                    for (int j = 0; j < maxVertexCount; j++) {
                        newAdjacencyMatrix[i][j] = adjacencyMatrix[i][j];
                    }
                }

                // Присваиваем новую матрицу и обновляем размер
                adjacencyMatrix = newAdjacencyMatrix;
            }

            maxVertexCount = newMaxVertexCount;
            resultLabel.setText("Теперь можно добавлять вершины. Количество: " + maxVertexCount);
        } catch (NumberFormatException e) {
            resultLabel.setText("Ошибка: введите корректное количество вершин (1-10).");
        }
    }

    // Метод добавления вершины
    public void addVertex(MouseEvent event) {
        for (Edge edge : edges) {
            edge.getLine().setStroke(Color.BLACK);
            edge.getArrow().setFill(Color.BLACK);
        }
        if (vertices.size() < maxVertexCount) {  // Ограничение на количество вершин
            double x = event.getX();
            double y = event.getY();
            Vertex vertex = new Vertex(vertices.size(), x, y);
            vertices.add(vertex);

            // Добавляем круг и номер вершины
            graphPane.getChildren().addAll(vertex.getCircle(), vertex.getText());

            // Добавляем обработчики событий для перетаскивания и кликов
            vertex.getCircle().setOnMousePressed(this::onVertexPressed);
            vertex.getCircle().setOnMouseDragged(this::onVertexDragged);
            vertex.getCircle().setOnMouseClicked(this::onVertexClicked);
        } else {
            resultLabel.setText("Достигнуто максимальное количество вершин: " + maxVertexCount);
        }
    }

    // Метод для обработки кликов по вершинам
    private void onVertexClicked(MouseEvent event) {
        Circle circle = (Circle) event.getSource();
        Vertex clickedVertex = null;

        // Поиск вершины, по которой кликнули
        for (Vertex vertex : vertices) {
            if (vertex.getCircle() == circle) {
                clickedVertex = vertex;
                break;
            }
        }

        // Обработка правого клика
        if (event.getButton() == MouseButton.SECONDARY) {
            if (clickedVertex.equals(selectedVertex)) {
                // Если кликнули по той же вершине, снимаем выделение
                clearSelection();
            } else {
                if (selectedVertex == null) {
                    // Если нет выделенной вершины, выделяем текущую
                    selectVertex(clickedVertex);
                } else {
                    // Если есть выделенная вершина, создаём ребро
                    addEdge(selectedVertex, clickedVertex);
                    clearSelection();
                }
            }
        } else if (event.getButton() == MouseButton.PRIMARY) {
            // Снятие выделения при левом клике
            clearSelection();
        }
    }

    // Метод для добавления ориентированного ребра между двумя вершинами с запросом веса
    public void addEdge(Vertex v1, Vertex v2) {
        if (v1.equals(v2)) return;  // Не добавляем ребро к самой себе

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Добавить ориентированное ребро");
        dialog.setHeaderText("Введите вес ребра:");
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            try {
                int weight = Integer.parseInt(result.get());
                if (weight > 0) {
                    Edge edge = new Edge(v1, v2, weight, this);  // Передаем ссылку на текущий контроллер
                    edges.add(edge);
                    graphPane.getChildren().addAll(edge.getLine(), edge.getWeightText(), edge.getArrow());

                    // Обновляем матрицу смежности
                    adjacencyMatrix[v1.getId()][v2.getId()] = weight;  // Только для v1 -> v2 (ориентированный граф)
                }
            } catch (NumberFormatException e) {
                resultLabel.setText("Ошибка: введите корректное значение веса.");
            }
        }
    }
    public void addEdge(Vertex v1, Vertex v2, int weight) {
        if (weight > 0) {
            // Поиск уже существующего ребра между этими вершинами
            Edge existingEdge = findEdge(v1, v2);
            if (existingEdge != null) {
                // Если ребро уже существует, просто обновляем его вес
                existingEdge.setWeight(weight);
            } else {
                // Если ребра нет, создаем новое
                Edge edge = new Edge(v1, v2, weight, this);  // Передаем ссылку на текущий контроллер
                edges.add(edge);
                graphPane.getChildren().addAll(edge.getLine(), edge.getWeightText(), edge.getArrow());
            }
            // Обновляем матрицу смежности
            adjacencyMatrix[v1.getId()][v2.getId()] = weight;
        }
    }
    // Метод для поиска ребра между двумя вершинами
    private Edge findEdge(Vertex v1, Vertex v2) {
        for (Edge edge : edges) {
            if (edge.getStart().equals(v1) && edge.getEnd().equals(v2)) {
                return edge;
            }
        }
        return null;
    }
    public void findShortestPath() {
        try {
            int start = Integer.parseInt(startVertexField.getText());
            int end = Integer.parseInt(endVertexField.getText());

            if (start < 0 || start >= maxVertexCount || end < 0 || end >= maxVertexCount) {
                resultLabel.setText("Ошибка: введите корректные номера вершин.");
                return;
            }

            DijkstraResult result = dijkstra(start, end);

            if (result.distance == Integer.MAX_VALUE) {
                resultLabel.setText("Путь между вершинами не существует.");
            } else {
                resultLabel.setText("Кратчайший путь длиной: " + result.distance + "\n" +
                        "Вершины: " + result.path);
                highlightPath(result.path);
            }
        } catch (NumberFormatException e) {
            resultLabel.setText("Ошибка: введите корректные номера вершин.");
        }
    }

    private DijkstraResult dijkstra(int start, int end) {
        int[] dist = new int[maxVertexCount];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        int[] prev = new int[maxVertexCount];
        Arrays.fill(prev, -1);

        PriorityQueue<VertexDist> pq = new PriorityQueue<>(Comparator.comparingInt(v -> v.distance));
        pq.add(new VertexDist(start, 0));

        while (!pq.isEmpty()) {
            VertexDist u = pq.poll();

            if (u.vertex == end) break;  // Достигли конечной вершины

            for (int v = 0; v < maxVertexCount; v++) {
                if (adjacencyMatrix[u.vertex][v] > 0) {
                    int alt = dist[u.vertex] + adjacencyMatrix[u.vertex][v];
                    if (alt < dist[v]) {
                        dist[v] = alt;
                        prev[v] = u.vertex;
                        pq.add(new VertexDist(v, alt));
                    }
                }
            }
        }

        // Построение пути
        List<Integer> path = new ArrayList<>();
        for (int at = end; at != -1; at = prev[at]) {
            path.add(at);
        }
        Collections.reverse(path);

        return new DijkstraResult(path.size() == 1 && path.get(0) != start ? Integer.MAX_VALUE : dist[end], path);
    }

    private void highlightPath(List<Integer> path) {
        // Снимаем выделение с предыдущих рёбер
        for (Edge edge : edges) {
            edge.getLine().setStroke(Color.BLACK);
            edge.getArrow().setFill(Color.BLACK);
        }

        // Выделяем путь
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            for (Edge edge : edges) {
                if (edge.getStart().getId() == from && edge.getEnd().getId() == to) {
                    edge.getLine().setStroke(Color.RED);
                    edge.getArrow().setFill(Color.RED);
                    break;
                }
            }
        }
    }


    // Вспомогательный класс для хранения результата Дейкстры
    private static class DijkstraResult {
        int distance;
        List<Integer> path;

        DijkstraResult(int distance, List<Integer> path) {
            this.distance = distance;
            this.path = path;
        }
    }

    // Вспомогательный класс для хранения вершин и их расстояний
    private static class VertexDist {
        int vertex;
        int distance;

        VertexDist(int vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }
    // Метод для отображения редактора матрицы
    public void showMatrixEditor() {
        Stage stage = new Stage();
        GridPane matrixPane = new GridPane();

        // Создаём текстовые поля для матрицы
        TextField[][] matrixFields = new TextField[maxVertexCount][maxVertexCount];
        for (int i = 0; i < maxVertexCount; i++) {
            for (int j = 0; j < maxVertexCount; j++) {
                TextField textField = new TextField(String.valueOf(adjacencyMatrix[i][j]));
                textField.setPrefWidth(40);
                matrixFields[i][j] = textField;
                matrixPane.add(textField, j, i);
            }
        }

        // Кнопка для сохранения изменений
        Button saveButton = new Button("Сохранить изменения");
        saveButton.setOnAction(event -> {
            boolean verticesAdded = vertices.isEmpty();  // Проверяем, добавлены ли уже вершины

            // Обновляем матрицу смежности из текстовых полей
            for (int i = 0; i < maxVertexCount; i++) {
                for (int j = 0; j < maxVertexCount; j++) {
                    try {
                        int weight = Integer.parseInt(matrixFields[i][j].getText());
                        adjacencyMatrix[i][j] = weight;

                        // Если вершины не были добавлены, строим их автоматически
                        if (verticesAdded) {
                            if (i < vertices.size()) continue;  // Вершина уже добавлена
                            Vertex vertex = new Vertex(i, 50 + i * 50, 50 + i * 50);
                            vertices.add(vertex);
                            graphPane.getChildren().addAll(vertex.getCircle(), vertex.getText());
                            vertex.getCircle().setOnMousePressed(this::onVertexPressed);
                            vertex.getCircle().setOnMouseDragged(this::onVertexDragged);
                            vertex.getCircle().setOnMouseClicked(this::onVertexClicked);
                        }

                        if (weight > 0) {  // Если ребро добавлено
                            addEdge(vertices.get(i), vertices.get(j), weight);
                        } else if (weight <= 0 && adjacencyMatrix[i][j] > 0) {  // Если ребро удалено
                            removeEdge(vertices.get(i), vertices.get(j));
                        }

                    } catch (NumberFormatException e) {
                        resultLabel.setText("Ошибка: некорректное значение в матрице.");
                    }
                }
            }
            stage.close();
        });

        matrixPane.add(saveButton, 0, maxVertexCount, maxVertexCount, 1);
        Scene scene = new Scene(matrixPane);
        stage.setScene(scene);
        stage.setTitle("Редактор матрицы смежности");
        stage.show();
    }

    // Метод для удаления ребра между вершинами
    public void removeEdge(Vertex v1, Vertex v2) {
        edges.removeIf(edge -> (edge.getStart().equals(v1) && edge.getEnd().equals(v2)));
        adjacencyMatrix[v1.getId()][v2.getId()] = 0;
        updateGraph();
    }

    // Метод для выделения вершины
    private void selectVertex(Vertex vertex) {
        selectedVertex = vertex;
        vertex.getCircle().setStroke(Color.RED);  // Изменяем цвет выделенной вершины
        vertex.getCircle().setStrokeWidth(3);
    }

    // Метод для снятия выделения
    private void clearSelection() {
        if (selectedVertex != null) {
            selectedVertex.getCircle().setStroke(Color.BLACK);  // Возвращаем цвет вершины
            selectedVertex.getCircle().setStrokeWidth(2);
            selectedVertex = null;
        }
    }

    // Метод для перетаскивания вершин
    private void onVertexPressed(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {  // Перетаскивание только левой кнопкой
            orgSceneX = event.getSceneX();
            orgSceneY = event.getSceneY();

            Circle circle = (Circle) event.getSource();

            // Поиск вершины, которой принадлежит этот круг
            Vertex vertex = null;
            for (Vertex v : vertices) {
                if (v.getCircle() == circle) {
                    vertex = v;
                    break;
                }
            }

            if (vertex != null) {
                vertex.getText().toFront();  // Перемещаем текст вершины на передний план
            }

            clearSelection();  // Снимаем выделение при перетаскивании
        }
    }

    private void onVertexDragged(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {  // Перетаскивание только левой кнопкой
            double offsetX = event.getSceneX() - orgSceneX;
            double offsetY = event.getSceneY() - orgSceneY;

            Circle circle = (Circle) event.getSource();
            circle.setCenterX(circle.getCenterX() + offsetX);
            circle.setCenterY(circle.getCenterY() + offsetY);

            orgSceneX = event.getSceneX();
            orgSceneY = event.getSceneY();

            // Обновляем положение номера вершины
            Vertex draggedVertex = null;
            for (Vertex vertex : vertices) {
                if (vertex.getCircle() == circle) {
                    draggedVertex = vertex;
                    break;
                }
            }
            if (draggedVertex != null) {
                draggedVertex.updateTextPosition();
            }

            updateEdges();  // Обновляем положение рёбер при перемещении вершин
        }
    }

    // Метод обновления положения рёбер
    private void updateEdges() {
        for (Edge edge : edges) {
            edge.updatePosition();
        }
    }

    // Метод обновления графа
    private void updateGraph() {
        graphPane.getChildren().clear();
        for (Vertex vertex : vertices) {
            graphPane.getChildren().addAll(vertex.getCircle(), vertex.getText());
        }
        for (Edge edge : edges) {
            graphPane.getChildren().addAll(edge.getLine(), edge.getWeightText(), edge.getArrow());
        }
    }

    // Внутренний класс для представления вершины
    public static class Vertex {
        private int id;
        private Circle circle;
        private Text text;

        public Vertex(int id, double x, double y) {
            this.id = id;
            this.circle = new Circle(x, y, 15, Color.LIGHTBLUE);
            this.circle.setStroke(Color.BLACK);
            this.circle.setStrokeWidth(2);

            // Текст с номером вершины
            this.text = new Text(x - 5, y + 5, String.valueOf(id));
        }

        public void toFront() {
            this.circle.toFront();
            this.text.toFront();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return id == vertex.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        public int getId() {
            return id;
        }

        public Circle getCircle() {
            return circle;
        }

        public Text getText() {
            return text;
        }

        // Метод для обновления положения текста при перемещении вершины
        public void updateTextPosition() {
            text.setX(circle.getCenterX() - 5);
            text.setY(circle.getCenterY() + 5);
            text.toFront();
        }
    }

    // Внутренний класс для представления дуги (ребра)
    public static class Edge {
        private Vertex start;
        private Vertex end;
        private Line line;
        private int weight;
        private Text weightText;
        private Polygon arrow;  // Добавляем стрелку для отображения направления
        private GraphController controller;  // Ссылка на GraphController

        public Edge(Vertex start, Vertex end, int weight, GraphController controller) {
            this.start = start;
            this.end = end;
            this.weight = weight;
            this.controller = controller;  // Сохраняем ссылку на контроллер
            this.line = new Line();
            this.weightText = new Text();
            this.arrow = new Polygon();  // Полигон для стрелки

            updatePosition();
            updateWeightText();

            // Добавляем обработчик двойного клика для изменения веса
            this.line.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(this.weight));
                    dialog.setTitle("Изменить вес");
                    dialog.setHeaderText("Введите новый вес:");
                    Optional<String> result = dialog.showAndWait();

                    if (result.isPresent()) {
                        try {
                            int newWeight = Integer.parseInt(result.get());
                            if (newWeight <= 0) {
                                // Если вес 0 или меньше, удаляем ребро
                                removeEdge();
                            } else {
                                this.weight = newWeight;
                                updateWeightText();
                            }
                        } catch (NumberFormatException e) {
                            // Игнорируем некорректный ввод
                        }
                    }
                }
            });
        }

        public Text getWeightText() {
            return this.weightText;
        }

        public Line getLine() {
            return line;
        }

        public Vertex getStart() {
            return start;
        }

        public Vertex getEnd() {
            return end;
        }

        public Polygon getArrow() {
            return arrow;
        }
        public void setWeight(int newWeight) {
            this.weight = newWeight;
            updateWeightText();  // Обновляем текст веса
        }

        // Обновляем координаты линии, стрелки и текста веса
        public void updatePosition() {
            line.setStartX(start.getCircle().getCenterX());
            line.setStartY(start.getCircle().getCenterY());
            line.setEndX(end.getCircle().getCenterX());
            line.setEndY(end.getCircle().getCenterY());

            weightText.setX((start.getCircle().getCenterX() + end.getCircle().getCenterX()) / 2);
            weightText.setY((start.getCircle().getCenterY() + end.getCircle().getCenterY()) / 2);
            weightText.toFront();

            // Обновляем положение стрелки
            updateArrow();
        }

        // Метод для обновления текста веса
        public void updateWeightText() {
            weightText.setText(String.valueOf(weight));
            controller.adjacencyMatrix[start.getId()][end.getId()] = weight;
        }

        // Метод для обновления положения и формы стрелки
        private void updateArrow() {
            double arrowLength = 10;  // Длина стрелки
            double arrowWidth = 7;  // Ширина стрелки

            double ex = end.getCircle().getCenterX();
            double ey = end.getCircle().getCenterY();
            double sx = start.getCircle().getCenterX();
            double sy = start.getCircle().getCenterY();

            double angle = Math.atan2(ey - sy, ex - sx);  // Вычисляем угол

            // Координаты точек стрелки
            double x1 = ex - arrowLength * Math.cos(angle - Math.PI / 6);
            double y1 = ey - arrowLength * Math.sin(angle - Math.PI / 6);
            double x2 = ex - arrowLength * Math.cos(angle + Math.PI / 6);
            double y2 = ey - arrowLength * Math.sin(angle + Math.PI / 6);

            arrow.getPoints().clear();
            arrow.getPoints().addAll(ex, ey, x1, y1, x2, y2);
        }

        // Удаляем ребро
        private void removeEdge() {
            controller.graphPane.getChildren().removeAll(line, weightText, arrow);
            controller.edges.remove(this);

            // Обновляем матрицу смежности
            controller.adjacencyMatrix[start.getId()][end.getId()] = 0;
        }
    }
}
