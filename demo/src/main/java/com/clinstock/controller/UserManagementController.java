package com.clinstock.controller;

import com.clinstock.dao.UserDAO;
import com.clinstock.model.User;
import com.clinstock.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

/**
 * Controller untuk halaman Kelola Akun.
 * Manajer klinik dapat menambah, mengubah, dan menghapus akun pengguna.
 */
public class UserManagementController {

    @FXML private TextField txtSearch;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colCreatedAt;
    @FXML private TableColumn<User, Void> colAction;
    @FXML private Label lblStatus;
    @FXML private Label lblPageInfo;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private final UserDAO userDAO;
    private static final int PAGE_SIZE = 15;
    private int currentPage = 0;
    private List<User> filteredUsers;

    public UserManagementController() {
        this.userDAO = new UserDAO();
    }

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !"manajer_klinik".equals(currentUser.getRole())) {
            tableUsers.setDisable(true);
            showStatus("Hanya Manajer Klinik yang dapat mengelola akun.", false);
            return;
        }

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleDisplayName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusDisplayName"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        setupActionColumn();
        refreshData();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Hapus");
            private final HBox container = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-edit");
                btnDelete.getStyleClass().add("btn-danger");

                btnEdit.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEdit(user);
                });

                btnDelete.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDelete(user);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                User user = getTableView().getItems().get(getIndex());
                btnDelete.setDisable(!canDelete(user));
                setGraphic(container);
            }
        });
    }

    private void refreshData() {
        filteredUsers = userDAO.getAllUsers();
        currentPage = 0;
        applyPagination();
    }

    private void applyPagination() {
        int totalItems = filteredUsers.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        tableUsers.setItems(FXCollections.observableArrayList(filteredUsers.subList(fromIndex, toIndex)));
        lblPageInfo.setText("Halaman " + (currentPage + 1) + " dari " + totalPages
                + " (" + totalItems + " akun)");
        btnPrev.setDisable(currentPage <= 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        filteredUsers = keyword.isEmpty() ? userDAO.getAllUsers() : userDAO.searchUsers(keyword);
        currentPage = 0;
        applyPagination();
    }

    @FXML
    private void handlePrev() {
        if (currentPage > 0) {
            currentPage--;
            applyPagination();
        }
    }

    @FXML
    private void handleNext() {
        currentPage++;
        applyPagination();
    }

    @FXML
    private void handleAdd() {
        showUserDialog(null);
    }

    private void handleEdit(User user) {
        showUserDialog(user);
    }

    private void handleDelete(User user) {
        if (!canDelete(user)) {
            showStatus("Akun manajer awal atau akun sendiri tidak dapat dihapus.", false);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText("Hapus akun: " + user.getUsername() + "?");
        confirm.setContentText("Data akun yang dihapus tidak dapat dikembalikan.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = userDAO.deleteUser(user.getId());
            showStatus(success ? "Akun berhasil dihapus." : "Gagal menghapus akun.", success);
            if (success) refreshData();
        }
    }

    private void showUserDialog(User existingUser) {
        boolean isEdit = existingUser != null;
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Akun" : "Tambah Akun Baru");

        ButtonType saveButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username login");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText(isEdit ? "Kosongkan jika tidak diubah" : "Password login");
        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Nama lengkap");
        ComboBox<RoleOption> cmbRole = new ComboBox<>();
        cmbRole.setItems(FXCollections.observableArrayList(
                new RoleOption("admin_farmasi", "Admin Farmasi"),
                new RoleOption("petugas_klinik", "Petugas Klinik"),
                new RoleOption("manajer_klinik", "Manajer Klinik")
        ));
        CheckBox chkActive = new CheckBox("Aktif");
        chkActive.setSelected(true);

        if (isEdit) {
            txtUsername.setText(existingUser.getUsername());
            txtFullName.setText(existingUser.getFullName());
            cmbRole.getSelectionModel().select(findRoleOption(cmbRole, existingUser.getRole()));
            chkActive.setSelected(existingUser.isActive());

            if (isInitialManager(existingUser)) {
                txtUsername.setDisable(true);
                cmbRole.setDisable(true);
                chkActive.setDisable(true);
            }
        } else {
            cmbRole.getSelectionModel().select(0);
        }

        grid.add(new Label("Username:"), 0, 0);    grid.add(txtUsername, 1, 0);
        grid.add(new Label("Password:"), 0, 1);    grid.add(txtPassword, 1, 1);
        grid.add(new Label("Nama Lengkap:"), 0, 2); grid.add(txtFullName, 1, 2);
        grid.add(new Label("Role:"), 0, 3);        grid.add(cmbRole, 1, 3);
        grid.add(new Label("Status:"), 0, 4);      grid.add(chkActive, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) return null;

            User user = isEdit ? existingUser : new User();
            user.setUsername(txtUsername.getText().trim());
            user.setPassword(txtPassword.getText());
            user.setFullName(txtFullName.getText().trim());
            RoleOption selectedRole = cmbRole.getSelectionModel().getSelectedItem();
            user.setRole(selectedRole != null ? selectedRole.value() : "");
            user.setActive(chkActive.isSelected());
            return user;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(user -> saveUser(user, isEdit));
    }

    private void saveUser(User user, boolean isEdit) {
        if (user.getUsername().isEmpty() || user.getFullName().isEmpty() || user.getRole().isEmpty()) {
            showStatus("Username, nama lengkap, dan role wajib diisi.", false);
            return;
        }

        boolean hasPasswordInput = user.getPassword() != null && !user.getPassword().isBlank();
        if (!isEdit && !hasPasswordInput) {
            showStatus("Password wajib diisi untuk akun baru.", false);
            return;
        }

        if (isEdit && isInitialManager(user)) {
            user.setUsername(UserDAO.INITIAL_MANAGER_USERNAME);
            user.setRole("manajer_klinik");
            user.setActive(true);
        }

        boolean success = isEdit
                ? userDAO.updateUser(user, hasPasswordInput)
                : userDAO.insertUser(user);

        showStatus(success
                ? (isEdit ? "Akun berhasil diperbarui." : "Akun baru berhasil ditambahkan.")
                : "Gagal menyimpan akun. Username mungkin sudah digunakan.",
                success);
        if (success) refreshData();
    }

    private boolean canDelete(User user) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (user == null || currentUser == null) return false;
        if (user.getId() == currentUser.getId()) return false;
        return !isInitialManager(user);
    }

    private boolean isInitialManager(User user) {
        return user != null && UserDAO.INITIAL_MANAGER_USERNAME.equalsIgnoreCase(user.getUsername());
    }

    private RoleOption findRoleOption(ComboBox<RoleOption> comboBox, String role) {
        for (RoleOption option : comboBox.getItems()) {
            if (option.value().equals(role)) return option;
        }
        return null;
    }

    private void showStatus(String message, boolean success) {
        lblStatus.setText(message);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
        lblStatus.getStyleClass().removeAll("status-success", "status-error");
        lblStatus.getStyleClass().add(success ? "status-success" : "status-error");
    }

    private record RoleOption(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
