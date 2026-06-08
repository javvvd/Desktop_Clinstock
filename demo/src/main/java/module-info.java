module com.clinstock {
    // ---- Module Dependencies ----
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    // ---- Export Libraries ----
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires itextpdf;

    // ---- Open packages ke javafx.fxml (FXMLLoader) ----
    opens com.clinstock to javafx.fxml;
    opens com.clinstock.controller to javafx.fxml;

    // ---- Open model ke javafx.base (PropertyValueFactory / TableView) ----
    opens com.clinstock.model to javafx.base;

    // ---- Export packages ----
    exports com.clinstock;
    exports com.clinstock.model;
    exports com.clinstock.controller;
    exports com.clinstock.dao;
    exports com.clinstock.util;
}
