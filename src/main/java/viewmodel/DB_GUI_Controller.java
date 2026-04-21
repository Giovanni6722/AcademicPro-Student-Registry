package viewmodel;

import dao.DbConnectivityClass;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Major;
import model.Person;
import service.MyLogger;
import service.UserSession;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class DB_GUI_Controller implements Initializable {

    // ── Form fields ───────────────────────────────────────────────────────────
    @FXML private TextField       first_name, last_name, department, email, imageURL;
    @FXML private ComboBox<Major> majorComboBox;

    // ── Profile image ─────────────────────────────────────────────────────────
    @FXML private ImageView img_view;

    // ── Menu bar + menu items ─────────────────────────────────────────────────
    @FXML private MenuBar  menuBar;
    @FXML private MenuItem editItem, deleteItem;

    // ── Table view ────────────────────────────────────────────────────────────
    @FXML private TableView<Person>             tv;
    @FXML private TableColumn<Person, Integer>  tv_id;
    @FXML private TableColumn<Person, String>   tv_fn, tv_ln, tv_department, tv_major, tv_email;

    // ── Buttons ───────────────────────────────────────────────────────────────
    @FXML private Button addBtn, editBtn, deleteBtn;

    // ── Status bar ────────────────────────────────────────────────────────────
    @FXML private Label statusLabel, recordCountLabel, dbModeLabel;

    // ── Search ────────────────────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── Validation regex ──────────────────────────────────────────────────────
    private static final Pattern NAME_PATTERN  =
            Pattern.compile("^[A-Za-z][A-Za-z'\\-]{1,24}$");
    private static final Pattern DEPT_PATTERN  =
            Pattern.compile("^[A-Za-z][A-Za-z\\s&/()]{1,49}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9._%+\\-]{0,62}[a-zA-Z0-9])?@"
                    + "[a-zA-Z0-9][a-zA-Z0-9.\\-]{0,251}\\.[a-zA-Z]{2,6}$");
    private static final Pattern URL_PATTERN   =
            Pattern.compile("^(https?://[^\\s]{5,500})?$");

    // ── Data ──────────────────────────────────────────────────────────────────
    private final DbConnectivityClass    cnUtil       = new DbConnectivityClass();
    private final ObservableList<Person> data         = cnUtil.getData();
    private       FilteredList<Person>   filteredData;

    // ── Initialise ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Table column bindings
        tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
        tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
        tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Filtered + sorted wrapper for real-time search
        filteredData = new FilteredList<>(data, p -> true);
        SortedList<Person> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tv.comparatorProperty());
        tv.setItems(sortedData);

        // ComboBox for Major enum
        majorComboBox.setItems(FXCollections.observableArrayList(Major.values()));
        majorComboBox.getSelectionModel().selectFirst();

        // UI state: Edit/Delete disabled until a row is selected
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);
        if (editItem   != null) editItem.setDisable(true);
        if (deleteItem != null) deleteItem.setDisable(true);
        addBtn.setDisable(true);   // enabled only when form is valid

        tv.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean has = sel != null;
            editBtn.setDisable(!has);
            deleteBtn.setDisable(!has);
            if (editItem   != null) editItem.setDisable(!has);
            if (deleteItem != null) deleteItem.setDisable(!has);
        });

        // Form validation on every keystroke
        first_name.textProperty().addListener((o, old, n) -> validateForm());
        last_name.textProperty().addListener((o, old, n)  -> validateForm());
        department.textProperty().addListener((o, old, n) -> validateForm());
        email.textProperty().addListener((o, old, n)      -> validateForm());
        imageURL.textProperty().addListener((o, old, n)   -> validateForm());
        majorComboBox.valueProperty().addListener((o, old, n) -> validateForm());

        // Real-time search
        searchField.textProperty().addListener((obs, old, query) -> {
            filteredData.setPredicate(p -> {
                if (query == null || query.isBlank()) return true;
                String q = query.toLowerCase().trim();
                return p.getFirstName().toLowerCase().contains(q)
                        || p.getLastName().toLowerCase().contains(q)
                        || p.getEmail().toLowerCase().contains(q)
                        || (p.getDepartment() != null && p.getDepartment().toLowerCase().contains(q))
                        || (p.getMajor()      != null && p.getMajor().toLowerCase().contains(q));
            });
            updateRecordCount();
        });

        // Inline editing (Extra Credit)
        tv.setEditable(true);
        setupInlineEditing();

        // DB mode indicator
        if (dbModeLabel != null) {
            dbModeLabel.setText(cnUtil.isUsingDerby() ? "DB: Derby (local)" : "DB: Azure MySQL");
        }

        updateRecordCount();
    }

    private void setupInlineEditing() {
        tv_fn.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_fn.setOnEditCommit(e -> {
            if (!NAME_PATTERN.matcher(e.getNewValue()).matches()) {
                showStatus("Invalid first name – edit rejected.", true);
                tv.refresh(); return;
            }
            e.getRowValue().setFirstName(e.getNewValue());
            cnUtil.editUser(e.getRowValue().getId(), e.getRowValue());
            showStatus("First name updated inline.", false);
        });

        tv_ln.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_ln.setOnEditCommit(e -> {
            if (!NAME_PATTERN.matcher(e.getNewValue()).matches()) {
                showStatus("Invalid last name – edit rejected.", true);
                tv.refresh(); return;
            }
            e.getRowValue().setLastName(e.getNewValue());
            cnUtil.editUser(e.getRowValue().getId(), e.getRowValue());
            showStatus("Last name updated inline.", false);
        });

        tv_email.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_email.setOnEditCommit(e -> {
            if (!EMAIL_PATTERN.matcher(e.getNewValue()).matches()) {
                showStatus("Invalid email format – edit rejected.", true);
                tv.refresh(); return;
            }
            e.getRowValue().setEmail(e.getNewValue());
            cnUtil.editUser(e.getRowValue().getId(), e.getRowValue());
            showStatus("Email updated inline.", false);
        });

        tv_department.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_department.setOnEditCommit(e -> {
            if (!DEPT_PATTERN.matcher(e.getNewValue()).matches()) {
                showStatus("Invalid department – edit rejected.", true);
                tv.refresh(); return;
            }
            e.getRowValue().setDepartment(e.getNewValue());
            cnUtil.editUser(e.getRowValue().getId(), e.getRowValue());
            showStatus("Department updated inline.", false);
        });
    }

    // ── Validation helpers ────────────────────────────────────────────────────

    private void validateForm() {
        addBtn.setDisable(!isFormValid());
        applyFieldStyle(first_name,  NAME_PATTERN.matcher(first_name.getText()).matches());
        applyFieldStyle(last_name,   NAME_PATTERN.matcher(last_name.getText()).matches());
        applyFieldStyle(department,  DEPT_PATTERN.matcher(department.getText()).matches());
        applyFieldStyle(email,       EMAIL_PATTERN.matcher(email.getText()).matches());
        applyFieldStyle(imageURL,    URL_PATTERN.matcher(imageURL.getText()).matches());
    }

    private boolean isFormValid() {
        return NAME_PATTERN.matcher(first_name.getText()).matches()
                && NAME_PATTERN.matcher(last_name.getText()).matches()
                && DEPT_PATTERN.matcher(department.getText()).matches()
                && EMAIL_PATTERN.matcher(email.getText()).matches()
                && URL_PATTERN.matcher(imageURL.getText()).matches()
                && majorComboBox.getValue() != null;
    }

    private void applyFieldStyle(TextField field, boolean valid) {
        if (field.getText().isEmpty()) { field.setStyle(""); return; }
        field.setStyle(valid
                ? "-fx-border-color: #4CAF50; -fx-border-width: 0 0 2 0;"
                : "-fx-border-color: #F44336; -fx-border-width: 0 0 2 0;");
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private void showStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            if (statusLabel == null) return;
            statusLabel.setText(message);
            statusLabel.setStyle(isError
                    ? "-fx-text-fill: #F44336; -fx-font-weight: bold;"
                    : "-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            statusLabel.setOpacity(1.0);
            FadeTransition ft = new FadeTransition(Duration.seconds(0.6), statusLabel);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setDelay(Duration.seconds(3.5));
            ft.setOnFinished(e -> statusLabel.setText("Ready"));
            ft.play();
        });
    }

    private void updateRecordCount() {
        if (recordCountLabel == null) return;
        int total    = data.size();
        int filtered = filteredData.size();
        recordCountLabel.setText(total == filtered
                ? "Records: " + total
                : filtered + " / " + total + " shown");
    }

    // ── CRUD actions ──────────────────────────────────────────────────────────

    @FXML
    protected void addNewRecord() {
        if (!isFormValid()) return;
        Person p = new Person(
                first_name.getText().trim(), last_name.getText().trim(),
                department.getText().trim(), majorComboBox.getValue().name(),
                email.getText().trim(), imageURL.getText().trim());
        cnUtil.insertUser(p);
        p.setId(cnUtil.retrieveId(p));
        data.add(p);
        clearForm();
        updateRecordCount();
        showStatus("✔ Added: " + p.getFirstName() + " " + p.getLastName(), false);
    }

    @FXML
    protected void editRecord() {
        Person selected = tv.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select a record to edit.", true); return; }
        if (!isFormValid())    { showStatus("Fix form errors before saving.", true); return; }

        int index = data.indexOf(selected);
        Person updated = new Person(
                selected.getId(),
                first_name.getText().trim(), last_name.getText().trim(),
                department.getText().trim(), majorComboBox.getValue().name(),
                email.getText().trim(), imageURL.getText().trim());
        cnUtil.editUser(selected.getId(), updated);
        data.set(index, updated);
        tv.getSelectionModel().select(index);
        showStatus("✔ Updated: " + updated.getFirstName() + " " + updated.getLastName(), false);
    }

    @FXML
    protected void deleteRecord() {
        Person selected = tv.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select a record to delete.", true); return; }

        // Confirmation dialog before destructive delete
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Student Record");
        alert.setContentText("Permanently delete "
                + selected.getFirstName() + " " + selected.getLastName()
                + "?\nThis action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int index = data.indexOf(selected);
            cnUtil.deleteRecord(selected);
            data.remove(index);
            clearForm();
            updateRecordCount();
            showStatus("✔ Deleted: " + selected.getFirstName() + " " + selected.getLastName(), false);
        }
    }

    @FXML
    protected void clearForm() {
        first_name.setText(""); last_name.setText("");
        department.setText(""); email.setText(""); imageURL.setText("");
        majorComboBox.getSelectionModel().selectFirst();
        tv.getSelectionModel().clearSelection();
        first_name.setStyle(""); last_name.setStyle("");
        department.setStyle(""); email.setStyle(""); imageURL.setStyle("");
        validateForm();
    }

    // ── CSV import / export ───────────────────────────────────────────────────

    @FXML
    protected void importCSV(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Students from CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fc.showOpenDialog(menuBar.getScene().getWindow());
        if (file == null) return;
        try {
            ObservableList<Person> imported = cnUtil.importFromCSV(file.getAbsolutePath());
            int count = 0;
            for (Person p : imported) {
                try {
                    cnUtil.insertUser(p);
                    p.setId(cnUtil.retrieveId(p));
                    data.add(p);
                    count++;
                } catch (Exception dup) {
                    MyLogger.makeLog("Skipped duplicate: " + p.getEmail());
                }
            }
            updateRecordCount();
            showStatus("✔ Imported " + count + " records from CSV.", false);
        } catch (Exception e) {
            showStatus("✘ CSV import failed: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    protected void exportCSV(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Students to CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("students_" + java.time.LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) return;
        try {
            cnUtil.exportToCSV(file.getAbsolutePath(), data);
            showStatus("✔ Exported " + data.size() + " records to CSV.", false);
        } catch (Exception e) {
            showStatus("✘ CSV export failed: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // ── PDF report (Extra Credit) ─────────────────────────────────────────────

    @FXML
    protected void generatePDFReport(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("academic_pro_report_" + java.time.LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) return;
        try {
            cnUtil.generatePDFReport(file.getAbsolutePath(), data);
            showStatus("✔ PDF report saved: " + file.getName(), false);
        } catch (Exception e) {
            showStatus("✘ PDF generation failed: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        UserSession.cleanInstance();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    protected void closeApplication() { System.exit(0); }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            stage.setTitle("About AcademicPro Student Registry");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Theme switchers ───────────────────────────────────────────────────────

    @FXML
    public void lightTheme(ActionEvent actionEvent) {
        applyTheme("/css/lightTheme.css");
        showStatus("Light theme applied.", false);
    }

    @FXML
    public void darkTheme(ActionEvent actionEvent) {
        applyTheme("/css/darkTheme.css");
        showStatus("Dark theme applied.", false);
    }

    private void applyTheme(String cssPath) {
        try {
            Scene scene = menuBar.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Row selection ─────────────────────────────────────────────────────────

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p == null) return;
        first_name.setText(p.getFirstName());
        last_name.setText(p.getLastName());
        department.setText(p.getDepartment() != null ? p.getDepartment() : "");
        majorComboBox.setValue(Major.fromString(p.getMajor()));
        email.setText(p.getEmail());
        imageURL.setText(p.getImageURL() != null ? p.getImageURL() : "");
        if (p.getImageURL() != null && !p.getImageURL().isBlank()) {
            try { img_view.setImage(new Image(p.getImageURL())); } catch (Exception ignored) {}
        }
    }

    // ── Image picker ──────────────────────────────────────────────────────────

    @FXML
    protected void showImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            String uri = file.toURI().toString();
            img_view.setImage(new Image(uri));
            imageURL.setText(uri);
        }
    }

    // ── Quick-add dialog ──────────────────────────────────────────────────────

    @FXML
    protected void addRecord() { showSomeone(); }

    public void showSomeone() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Quick Add Student");
        dialog.setHeaderText("Enter basic student info:");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tf1 = new TextField(); tf1.setPromptText("First Name");
        TextField tf2 = new TextField(); tf2.setPromptText("Last Name");
        TextField tf3 = new TextField(); tf3.setPromptText("Email");
        TextField tf4 = new TextField(); tf4.setPromptText("Department");
        ComboBox<Major> cb = new ComboBox<>(FXCollections.observableArrayList(Major.values()));
        cb.getSelectionModel().selectFirst();
        pane.setContent(new VBox(8, tf1, tf2, tf3, tf4, cb));
        Platform.runLater(tf1::requestFocus);
        dialog.setResultConverter(btn -> null);
        dialog.showAndWait();

        if (!tf1.getText().isBlank() && !tf3.getText().isBlank()) {
            first_name.setText(tf1.getText());
            last_name.setText(tf2.getText());
            department.setText(tf4.getText());
            email.setText(tf3.getText());
            majorComboBox.setValue(cb.getValue());
        }
    }
}