package lk.ijse.dep10.editor.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lk.ijse.dep10.editor.util.SearchResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditSceneController {

    public Label lblTitle;
    public Button btnMinimize;
    public Button btnResizable;
    public Button btnExit;
    public AnchorPane root;
    public MenuItem mnSave;
    public Label lblWords;
    public Label lblCharactors;
    public Label lblBytes;
    public TextField txtFind;
    public Button btnUp;
    public Button btnDown;
    public CheckBox chkMatchCase;
    public TextField txtReplace;
    public Button btnReplace;
    public Button btnReplaceAll;
    public Label lblResult;
    public TextArea txtEditor;
    public String fileName;
    public File file = null;
    public ArrayList<SearchResult> searchResultList = new ArrayList<>();
    public int pos = 0;
    public boolean flag = false;
    String replaceText = null;
    Matcher matcher;
    private double axisX;
    private double axisY;
    private Stage stage = null;
    private boolean flagSave = true;

    public void initialize() {
        lblTitle.setText("UnTitle Document");

        txtEditor.textProperty().addListener((value, previous, current) -> {
            if (fileName == null) {
                lblTitle.setText("*UnTitle Document");
            } else {
                lblTitle.setText("*".concat(fileName));
            }
            String line = txtEditor.getText().strip();
            bottomBar(line.getBytes());
            flagSave = false;
        });

        txtFind.textProperty().addListener((value, previous, current) -> wordCount());

        txtReplace.textProperty().addListener((value, previous, current) -> replaceText = current);

        txtEditor.textProperty().addListener((value, previous, current) -> wordCount());

        chkMatchCase.selectedProperty().addListener((value, previous, current) -> {
            flag = !chkMatchCase.isSelected();
            wordCount();
        });

    }

    private void wordCount() {
        String query = txtFind.getText();
        txtEditor.deselect();
        searchResultList.clear();
        pos = 0;


        if (query.isEmpty()) {
            lblResult.setText("0 Results");
            return;
        }


        Pattern pattern;
        try {
            if (flag) {
                pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            } else {
                pattern = Pattern.compile(query);
            }
        } catch (RuntimeException e) {
            return;
        }
        matcher = pattern.matcher(txtEditor.getText());
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            searchResultList.add(new SearchResult(start, end));
        }

        lblResult.setText(searchResultList.size() + " Results");

        select();
    }

    private void select() {
        if (searchResultList.isEmpty()) return;

        SearchResult searchResult = searchResultList.get(pos);
        txtEditor.selectRange(searchResult.getStart(), searchResult.getEnd());
        lblResult.setText(String.format("%d/%d Results", (pos + 1), searchResultList.size()));
    }


    public void btnMinimizeOnAction(ActionEvent actionEvent) {
        stage = (Stage) root.getScene().getWindow();
        stage.setIconified(true);
    }

    public void btnResizableOnAction(ActionEvent actionEvent) {
        stage = (Stage) root.getScene().getWindow();

        stage.setMaximized(!stage.isMaximized());


    }

    public void btnExitOnAction(ActionEvent actionEvent) {
        stage = (Stage) root.getScene().getWindow();

        if (txtEditor.getText().isEmpty() && flagSave) {
            stage.close();
            return;
        }

        ButtonType btnNo = new ButtonType("NO");
        if (!flagSave) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save this?",
                    ButtonType.YES, btnNo);
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isEmpty()) {
                return;
            }
            if (result.get() == btnNo) {
                stage.close();
            } else if (result.get() == ButtonType.YES) {
                mnSave.fire();
                stage.close();
            }
        } else {
            stage.close();
        }
    }

    @FXML
    void mnAboutOnAction(ActionEvent event) throws IOException {
        Stage stageAbout = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/AboutScene.fxml"));
        AnchorPane root = fxmlLoader.load();

        stageAbout.setScene(new Scene(root));
        stageAbout.setTitle("About KC edit");
        stageAbout.initModality(Modality.WINDOW_MODAL);
        stageAbout.initOwner(btnExit.getScene().getWindow());
        stageAbout.centerOnScreen();
        stageAbout.show();

    }

    @FXML
    void mnCloseOnAction(ActionEvent event) {
        btnExit.fire();
    }

    @FXML
    void mnNewOnAction(ActionEvent event) {
        if (!flagSave) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save this?",
                    ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == ButtonType.NO) {
                txtEditor.clear();
                fileName = null;
                flagSave = false;
                file = null;
                lblTitle.setText("UnTitle Document");
            }
            if (result.get() == ButtonType.YES) {
                mnSave.fire();
            }
        }
        txtEditor.clear();
        fileName = null;
        flagSave = false;
        file = null;
        lblTitle.setText("UnTitle Document");

    }

    @FXML
    void mnOpenOnAction(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open a text file");
        file = fileChooser.showOpenDialog(txtEditor.getScene().getWindow());
        if (file == null) return;

        openFile(file);

    }

    @FXML
    void mnPrintOnAction(ActionEvent event) {

    }

    @FXML
    void mnSaveOnAction(ActionEvent event) throws IOException {
        if (file == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save a text file");
            file = fileChooser.showSaveDialog(txtEditor.getScene().getWindow());
            if (file == null) return;
        }

        save(file);
    }

    private void save(File file) throws IOException {

        lblTitle.setText(file.getName());

        FileOutputStream fos = new FileOutputStream(file);
        String text = txtEditor.getText();
        byte[] bytes = text.getBytes();
        fos.write(bytes);
        fos.close();
        flagSave = true;
    }

    public void mnSaveAsOnAction(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As a text file");
        file = fileChooser.showSaveDialog(txtEditor.getScene().getWindow());
        if (file == null) return;

        save(file);
    }


    public void rootOnDragOver(DragEvent dragEvent) {
        dragEvent.acceptTransferModes(TransferMode.ANY);
    }

    public void rootOnDragDropped(DragEvent dragEvent) throws IOException {

        File droppedFile = dragEvent.getDragboard().getFiles().get(0);
        openFile(droppedFile);

    }

    private void openFile(File selectFile) throws IOException {
        file = selectFile;
        fileName = file.getName();
        lblTitle.setText(file.getName());

        FileInputStream fis = new FileInputStream(selectFile);
        byte[] bytes = fis.readAllBytes();
        fis.close();

        bottomBar(bytes);

        txtEditor.setText(new String(bytes));
    }

    private void bottomBar(byte[] bytes) {
        lblBytes.setText("Bytes: " + bytes.length);
        lblCharactors.setText("Characters: " + bytes.length);
        String[] para = new String(bytes).split("\\s");
        ArrayList<String> words = new ArrayList<>();
        for (String s : para) {
            if (s.isEmpty()) {
                continue;
            }
            words.add(s);
        }

        lblWords.setText("Words: " + words.size());
    }


    public void rootOnMouseDragged(MouseEvent mouseEvent) {
        stage = (Stage) root.getScene().getWindow();

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            stage.setX(mouseEvent.getScreenX() - axisX);
            stage.setY(mouseEvent.getScreenY() - axisY);
            root.setCursor(Cursor.CLOSED_HAND);

        }

    }

    public void rootOnMousePressed(MouseEvent mouseEvent) {
        axisX = mouseEvent.getX();
        axisY = mouseEvent.getY();
    }

    public void rootOnMouseReleased(MouseEvent mouseEvent) {
        root.setCursor(Cursor.DEFAULT);
    }


    public void mnClearOnAction(ActionEvent actionEvent) {
        txtEditor.clear();
    }


    public void btnUpOnAction(ActionEvent actionEvent) {
        pos--;
        if (pos < 0) {
            pos = searchResultList.size();
            return;
        }
        select();
    }

    public void btnDownOnAction(ActionEvent actionEvent) {
        pos++;
        if (pos == searchResultList.size()) {
            pos = -1;
            return;
        }

        select();
    }

    public void btnReplaceAllOnAction(ActionEvent actionEvent) {
        if (replaceText == null) return;

        txtEditor.setText(matcher.replaceAll(replaceText));
    }

    public void btnReplaceOnAction(ActionEvent actionEvent) {
        if (replaceText == null) return;
        if (searchResultList.isEmpty()) return;
        if (txtEditor.getSelectedText() == null) return;

        txtEditor.replaceSelection(replaceText);
        wordCount();
    }
}