module org.example.lab12 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens org.example.lab12 to javafx.fxml;
    exports org.example.lab12;
}