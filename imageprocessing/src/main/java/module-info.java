module leap {
    requires javafx.controls;
    requires javafx.fxml;

    opens leap to javafx.fxml;
    exports leap;
}
