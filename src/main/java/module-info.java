module com.densitymst {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens com.densitymst to javafx.graphics;
    opens com.densitymst.controller to javafx.fxml;
    opens com.densitymst.model to com.google.gson;

    exports com.densitymst;
    exports com.densitymst.controller;
    exports com.densitymst.core;
    exports com.densitymst.model;
    exports com.densitymst.service;
    exports com.densitymst.view;
}
