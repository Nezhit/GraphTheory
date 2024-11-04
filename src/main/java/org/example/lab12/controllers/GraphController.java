package org.example.lab12.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
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
import javafx.util.Duration;
import org.example.lab12.classes.Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;


public class GraphController {

    @FXML
    private TextField vertexCountField, startVertexField, endVertexField,packetCountField;
    @FXML
    private Label resultLabel;
    @FXML
    private Pane graphPane;
    @FXML
    private ChoiceBox<String> routingMethodChoiceBox;

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
    @FXML
    private void onKeyPressed(javafx.scene.input.KeyEvent event) {
        if ((event.getCode() == javafx.scene.input.KeyCode.DELETE || event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) && selectedVertex != null) {
            removeVertex(selectedVertex);
            clearSelection();  // Снимаем выделение после удаления вершины
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
    // Метод для удаления вершины
    private void removeVertex(Vertex vertex) {
        int vertexId = vertex.getId();

        // Удаляем все рёбра, связанные с этой вершиной
        List<Edge> edgesToRemove = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getStart().equals(vertex) || edge.getEnd().equals(vertex)) {
                edgesToRemove.add(edge);
            }
        }
        for (Edge edge : edgesToRemove) {
            removeEdge(edge.getStart(), edge.getEnd());
        }

        // Удаляем вершину с графической панели
        graphPane.getChildren().removeAll(vertex.getCircle(), vertex.getText());

        // Удаляем вершину из списка вершин
        vertices.remove(vertex);

        // Обновляем матрицу смежности: сдвигаем строки и столбцы
        int[][] newAdjacencyMatrix = new int[maxVertexCount - 1][maxVertexCount - 1];
        for (int i = 0, newI = 0; i < maxVertexCount; i++) {
            if (i == vertexId) continue; // Пропускаем удаляемую вершину
            for (int j = 0, newJ = 0; j < maxVertexCount; j++) {
                if (j == vertexId) continue; // Пропускаем удаляемую вершину
                newAdjacencyMatrix[newI][newJ] = adjacencyMatrix[i][j];
                newJ++;
            }
            newI++;
        }

        // Обновляем матрицу и количество вершин
        adjacencyMatrix = newAdjacencyMatrix;
        maxVertexCount--;

        // Уменьшаем идентификаторы оставшихся вершин
        for (Vertex v : vertices) {
            if (v.getId() > vertexId) {
                v.setId(v.getId() - 1);
            }
        }

        // Перерисовываем граф
        updateGraph();
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
            graphPane.requestFocus();
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
    @FXML
    private void startRouting() {
        try {
            int startVertex = Integer.parseInt(startVertexField.getText());
            int endVertex = Integer.parseInt(endVertexField.getText());
            int packetCount = Integer.parseInt(packetCountField.getText());

            String routingMethod = routingMethodChoiceBox.getValue();
            List<Integer> route = findRoute(startVertex, endVertex); // метод для получения маршрута

            if (route.isEmpty()) {
                resultLabel.setText("Путь не найден.");
            } else {
                animatePackets(route, packetCount, routingMethod); // Запуск анимации пакетов
                resultLabel.setText("Маршрутизация завершена: " + routingMethod);
            }

        } catch (NumberFormatException e) {
            resultLabel.setText("Ошибка: введите корректные данные.");
        }
    }
    private List<Integer> findRoute(int start, int end) {
        List<Integer> route = new ArrayList<>();
        Random random = new Random();
        int current = start;

        while (current != end) {
            route.add(current);
            List<Integer> neighbors = new ArrayList<>();
            for (int i = 0; i < maxVertexCount; i++) {
                if (adjacencyMatrix[current][i] > 0) {
                    neighbors.add(i);
                }
            }

            if (neighbors.isEmpty()) {
                return new ArrayList<>();  // Путь не найден
            }

            current = neighbors.get(random.nextInt(neighbors.size()));
        }

        route.add(end);
        return route;
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
    public void animatePackets(List<Integer> route, int packetCount, String routingMethod) {
        List<Packet> packets = new ArrayList<>();

        // Создаём пакеты с указанной конечной точкой
        for (int i = 0; i < packetCount; i++) {
            Packet packet = new Packet(route.get(route.size() - 1)); // Конечная вершина маршрута
            packets.add(packet);
            graphPane.getChildren().add(packet.getPacketCircle()); // Добавляем визуализацию на панель
        }

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);

            // Устанавливаем начальную позицию пакета на первой вершине маршрута
            Vertex startVertex = vertices.get(route.get(0));
            packet.getPacketCircle().setCenterX(startVertex.getCircle().getCenterX());
            packet.getPacketCircle().setCenterY(startVertex.getCircle().getCenterY());
            packet.setCurrentPosition(route.get(0)); // Установка начальной позиции пакета

            // Добавляем задержку перед началом анимации для каждого пакета
            PauseTransition initialDelay = new PauseTransition(Duration.millis(i * 200));
            initialDelay.setOnFinished(e -> {
                // Запуск анимации для каждого пакета после задержки
                final int[] currentPositionIndex = {0};
                if (routingMethod.equals("дейтаграмма")) {//
                    playTransition(packet, findRoute(route.getFirst(),route.getLast()), currentPositionIndex, routingMethod);
                }else if(routingMethod.equals("виртуальный канал")){
                    playTransition(packet, route, currentPositionIndex, routingMethod);
                }

            });
            initialDelay.play();
        }
    }

    private void playTransition(Packet packet, List<Integer> route, int[] currentPositionIndex, String routingMethod) {
        if (currentPositionIndex[0] < route.size() - 1) {
            int nextPositionIndex;


                // последовательное перемещение для виртуального канала
                nextPositionIndex = currentPositionIndex[0] + 1;


            int currentVertexId = route.get(currentPositionIndex[0]);
            int nextVertexId = route.get(nextPositionIndex);

            Vertex currentVertex = vertices.get(currentVertexId);
            Vertex nextVertex = vertices.get(nextVertexId);

            // Вычисляем координаты текущей и следующей вершины
            double startX = currentVertex.getCircle().getCenterX();
            double startY = currentVertex.getCircle().getCenterY();
            double endX = nextVertex.getCircle().getCenterX();
            double endY = nextVertex.getCircle().getCenterY();

            // Обновляем позицию packetCircle для нового перехода
            Circle packetCircle = packet.getPacketCircle();
            packetCircle.setTranslateX(0);
            packetCircle.setTranslateY(0);
            packetCircle.setCenterX(startX);
            packetCircle.setCenterY(startY);

            // Создаем плавный переход между текущей и следующей вершинами
            TranslateTransition transition = new TranslateTransition(Duration.seconds(1), packetCircle);
            transition.setFromX(0);
            transition.setFromY(0);
            transition.setToX(endX - startX);
            transition.setToY(endY - startY);

            transition.setOnFinished(e -> {
                packet.setCurrentPosition(nextVertexId); // Обновляем текущую позицию пакета
                currentPositionIndex[0] = nextPositionIndex; // Обновляем индекс позиции
                playTransition(packet, route, currentPositionIndex, routingMethod); // Рекурсивно запускаем переход к следующей вершине
            });

            transition.play();
        } else {
            graphPane.getChildren().remove(packet.getPacketCircle()); // Убираем пакет после достижения цели
        }
    }


    // Вспомогательный метод для создания анимации перехода
    private void playTransition(Circle packetCircle, List<Integer> route, int[] currentPositionIndex, String routingMethod) {
        if (currentPositionIndex[0] < route.size() - 1) {
            int nextPositionIndex;

            if (routingMethod.equals("дейтаграмма")) {
                // случайный выбор следующей вершины для дейтаграммного метода
                nextPositionIndex = (int) (Math.random() * (route.size() - currentPositionIndex[0] - 1)) + currentPositionIndex[0] + 1;
            } else {
                // последовательное перемещение для виртуального канала
                nextPositionIndex = currentPositionIndex[0] + 1;
            }

            int currentVertexId = route.get(currentPositionIndex[0]);
            int nextVertexId = route.get(nextPositionIndex);

            Vertex currentVertex = vertices.get(currentVertexId);
            Vertex nextVertex = vertices.get(nextVertexId);

            // Вычисляем координаты следующей вершины
            double startX = currentVertex.getCircle().getCenterX();
            double startY = currentVertex.getCircle().getCenterY();
            double endX = nextVertex.getCircle().getCenterX();
            double endY = nextVertex.getCircle().getCenterY();

            // Создаем переход между текущей и следующей вершинами
            TranslateTransition transition = new TranslateTransition(Duration.seconds(1), packetCircle);
            transition.setFromX(startX - packetCircle.getCenterX()); // смещение относительно текущей позиции
            transition.setFromY(startY - packetCircle.getCenterY());
            transition.setToX(endX - packetCircle.getCenterX()); // смещение для следующей позиции
            transition.setToY(endY - packetCircle.getCenterY());

            // Переход к следующей вершине
            transition.setOnFinished(e -> {
                packetCircle.setCenterX(endX);
                packetCircle.setCenterY(endY);
                currentPositionIndex[0] = nextPositionIndex; // Обновляем индекс позиции
                playTransition(packetCircle, route, currentPositionIndex, routingMethod); // Рекурсивно запускаем переход к следующей вершине
            });

            transition.play();
        } else {
            graphPane.getChildren().remove(packetCircle); // Убираем пакет после достижения цели
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
    public void findShortestPathFloyd() {
        try {
            int start = Integer.parseInt(startVertexField.getText());
            int end = Integer.parseInt(endVertexField.getText());

            if (start < 0 || start >= maxVertexCount || end < 0 || end >= maxVertexCount) {
                resultLabel.setText("Ошибка: введите корректные номера вершин.");
                return;
            }

            // Вызов алгоритма Флойда-Уоршалла
            FloydResult result = floydWarshall(start, end);

            if (result.distance == Integer.MAX_VALUE) {
                resultLabel.setText("Путь между вершинами не существует.");
            } else {
                resultLabel.setText("Алгоритм Флойда-Уоршалла: Кратчайший путь длиной: " + result.distance + "\n" +
                        "Вершины: " + result.path);
                highlightPath(result.path);  // Выделяем путь на графе
            }
        } catch (NumberFormatException e) {
            resultLabel.setText("Ошибка: введите корректные номера вершин.");
        }
    }

    // Алгоритм Флойда-Уоршалла для поиска кратчайшего пути
    private FloydResult floydWarshall(int start, int end) {
        int[][] dist = new int[maxVertexCount][maxVertexCount];
        int[][] next = new int[maxVertexCount][maxVertexCount];

        // Инициализация матрицы расстояний
        for (int i = 0; i < maxVertexCount; i++) {
            for (int j = 0; j < maxVertexCount; j++) {
                if (i == j) {
                    dist[i][j] = 0;
                } else if (adjacencyMatrix[i][j] != 0) {
                    dist[i][j] = adjacencyMatrix[i][j];
                } else {
                    dist[i][j] = Integer.MAX_VALUE;
                }
                next[i][j] = (adjacencyMatrix[i][j] != 0) ? j : -1;
            }
        }

        // Основной цикл алгоритма Флойда-Уоршалла
        for (int k = 0; k < maxVertexCount; k++) {
            for (int i = 0; i < maxVertexCount; i++) {
                for (int j = 0; j < maxVertexCount; j++) {
                    if (dist[i][k] != Integer.MAX_VALUE && dist[k][j] != Integer.MAX_VALUE &&
                            dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        next[i][j] = next[i][k];
                    }
                }
            }
        }

        // Построение пути
        List<Integer> path = new ArrayList<>();
        if (next[start][end] == -1) {
            return new FloydResult(Integer.MAX_VALUE, path);  // Пути нет
        }

        int at = start;
        while (at != end) {
            path.add(at);
            at = next[at][end];
        }
        path.add(end);

        return new FloydResult(dist[start][end], path);
    }

    // Вспомогательный класс для результата алгоритма Флойда-Уоршалла
    private static class FloydResult {
        int distance;
        List<Integer> path;

        FloydResult(int distance, List<Integer> path) {
            this.distance = distance;
            this.path = path;
        }
    }
    // Метод для отображения всех кратчайших путей и сравнения алгоритмов
    public void showAllShortestPaths() {
        Stage stage = new Stage();
        GridPane gridPane = new GridPane();

        // Время выполнения алгоритма Дейкстры
        long startTimeDijkstra = System.nanoTime();
        String dijkstraResults = calculateAllPathsDijkstra();
        long endTimeDijkstra = System.nanoTime();
        long dijkstraTime = endTimeDijkstra - startTimeDijkstra;

        // Время выполнения алгоритма Флойда-Уоршалла
        long startTimeFloyd = System.nanoTime();
        String floydResults = calculateAllPathsFloyd();
        long endTimeFloyd = System.nanoTime();
        long floydTime = endTimeFloyd - startTimeFloyd;

        // Отображаем результаты
        Label dijkstraLabel = new Label("Алгоритм Дейкстры:\n" + dijkstraResults + "\nВремя выполнения: " + dijkstraTime + " нс");
        Label floydLabel = new Label("Алгоритм Флойда-Уоршалла:\n" + floydResults + "\nВремя выполнения: " + floydTime + " нс");

        gridPane.add(dijkstraLabel, 0, 0);
        gridPane.add(floydLabel, 0, 1);

        Scene scene = new Scene(gridPane, 600, 400);
        stage.setScene(scene);
        stage.setTitle("Сравнение алгоритмов Дейкстры и Флойда-Уоршалла");
        stage.show();
    }

    // Метод для вычисления всех кратчайших путей с использованием алгоритма Дейкстры
    private String calculateAllPathsDijkstra() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxVertexCount; i++) {
            for (int j = 0; j < maxVertexCount; j++) {
                if (i != j) {
                    DijkstraResult dijkstraResult = dijkstra(i, j);
                    result.append("Из ").append(i).append(" в ").append(j)
                            .append(": Длина пути = ").append(dijkstraResult.distance)
                            .append(", Путь = ").append(dijkstraResult.path).append("\n");
                }
            }
        }
        return result.toString();
    }

    // Метод для вычисления всех кратчайших путей с использованием алгоритма Флойда-Уоршалла
    private String calculateAllPathsFloyd() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxVertexCount; i++) {
            for (int j = 0; j < maxVertexCount; j++) {
                if (i != j) {
                    FloydResult floydResult = floydWarshall(i, j);
                    result.append("Из ").append(i).append(" в ").append(j)
                            .append(": Длина пути = ").append(floydResult.distance)
                            .append(", Путь = ").append(floydResult.path).append("\n");
                }
            }
        }
        return result.toString();
    }
    // Метод для отображения редактора матрицы
    public void showMatrixEditor() {
        Stage stage = new Stage();
        GridPane matrixPane = new GridPane();

        // Создаём текстовые поля для матрицы
        TextField[][] matrixFields = new TextField[maxVertexCount][maxVertexCount];

        for (int i = 0; i < maxVertexCount; i++) {
            for (int j = 0; j < maxVertexCount; j++) {
                // Инициализация текстовых полей перед их использованием
                TextField textField = new TextField(String.valueOf(adjacencyMatrix[i][j]));
                textField.setPrefWidth(40);
                matrixFields[i][j] = textField;

                // Проверяем, существуют ли вершины с id i и j, и настраиваем доступность полей
                int finalI = i;
                int finalJ = j;
                if (vertices.stream().anyMatch(v -> v.getId() == finalI) && vertices.stream().anyMatch(v -> v.getId() == finalJ)) {
                    textField.setDisable(false);
                } else {
                    textField.setDisable(true);
                }

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
        public void setId(int newId) {
            this.id = newId;
            text.setText(String.valueOf(newId));  // Обновляем отображение номера вершины
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
