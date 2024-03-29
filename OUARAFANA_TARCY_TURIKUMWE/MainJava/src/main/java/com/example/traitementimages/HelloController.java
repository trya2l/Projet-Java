package com.example.traitementimages;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class HelloController {

    private Picture currentPicture;
    private PictureDaoImpl images;
    Stage stage;
    @FXML
    private FileChooser fc;
    private ObservableList<String> imagesList = FXCollections.observableArrayList();
    private ObservableList<String> tagList = FXCollections.observableArrayList();
    private ObservableList<String> imageTagList = FXCollections.observableArrayList();
    @FXML
    private ListView<String> items, itemsTag, imageTags;
    @FXML
    private TextField searchTag, addTag;
    @FXML
    private ImageView imageView, noFilter, blueRedGreen, blackAndWhite, sepia, prewitt;
    @FXML
    private PasswordField password;

    @FXML
    protected void initialize() {
        images = new PictureDaoImpl();
        loadImages();
        items.getSelectionModel().select("placeholder-image.png");
        showImage();
        fc = new FileChooser();

        items.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                    addTag.setText(null);
                    getCurrentImage();
                    imageView.setImage(currentPicture.getFilteredImage());
                    noFilter.setImage(currentPicture.getImage());
                    blueRedGreen.setImage(currentPicture.toBRG());
                    blackAndWhite.setImage(currentPicture.toBlackAndWhite());
                    sepia.setImage(currentPicture.toSepia());
                    prewitt.setImage(currentPicture.toPrewitt());
                    if (currentPicture.isInvert()) {
                        imageView.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                    } else {
                        imageView.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                    }
                    rotation(currentPicture.getRotation());
                }
            }
        });

        searchTag.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (!(tagList.contains(searchTag.getText()))) {
                        tagList.add(searchTag.getText());
                        itemsTag.setItems(tagList);
                        imagesWithTags();
                        searchTag.setText(null);
                    }
                }
            }
        });
        addTag.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (!(imageTagList.contains(addTag.getText()))) {
                        images.getPictures().get(items.getSelectionModel().getSelectedIndex()).addTag(addTag.getText());
                        imageTagList.add(addTag.getText());
                        imageTags.setItems(imageTagList);
                        addTag.setText(null);
                    }
                }
            }
        });
    }

    @FXML
    protected void chooseFile() throws IOException {
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            if (imagesList.contains(file.getName())) {
                Alert sameName = new Alert(Alert.AlertType.INFORMATION, "Un fichier enregistré à ce nom existe déjà !");
                sameName.setTitle("Doublon détecté");
                sameName.setHeaderText(null);
                sameName.show();
                items.getSelectionModel().select(file.getName());
                showImage();
            } else {
                imagesList.add(file.getName());
                items.setItems(imagesList);
                Path source = Paths.get(file.getAbsolutePath());
                Path dest = Paths.get("src/main/resources/images/" + file.getName());
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                currentPicture = new Picture(new Image(dest.toFile().toURI().toURL().toString()), dest.toFile(), images.getPictures().size());
                images.addPicture(currentPicture);
                //Collections.sort(imagesList);
                imageView.setImage(currentPicture.getImage());
                noFilter.setImage(currentPicture.getImage());
                blueRedGreen.setImage(currentPicture.toBRG());
                blackAndWhite.setImage(currentPicture.toBlackAndWhite());
                sepia.setImage(currentPicture.toSepia());
                prewitt.setImage(currentPicture.toPrewitt());
                items.getSelectionModel().select(currentPicture.getFile().getName());
                rotation(0);
                save();
            }
        }
    }

    @FXML
    public void verticalInvert() {
        rotation(currentPicture.getRotation() + 180);
    }

    @FXML
    public void horizontalInvert() {
        currentPicture.setInvert(!currentPicture.isInvert());
        if (currentPicture.isInvert()) {
            imageView.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            imageView.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
    }

    @FXML
    public void rotateLeft() {
        rotation(currentPicture.getRotation() - 90);
    }

    @FXML
    public void rotateRight() {
        rotation(currentPicture.getRotation() + 90);
    }

    @FXML
    public void removeFilters() {
        currentPicture = images.filter(currentPicture, 0, false);
        imageView.setImage(currentPicture.getFilteredImage());
        noFilter.setImage(currentPicture.getImage());
        blueRedGreen.setImage(currentPicture.toBRG());
        blackAndWhite.setImage(currentPicture.toBlackAndWhite());
        sepia.setImage(currentPicture.toSepia());
        prewitt.setImage(currentPicture.toPrewitt());
    }

    @FXML
    public void blueRedGreenFilter() {
        currentPicture = images.filter(currentPicture, 1, false);
        imageView.setImage(currentPicture.getFilteredImage());
        noFilter.setImage(currentPicture.getImage());
        blueRedGreen.setImage(currentPicture.toBRG());
        blackAndWhite.setImage(currentPicture.toBlackAndWhite());
        sepia.setImage(currentPicture.toSepia());
        prewitt.setImage(currentPicture.toPrewitt());
    }

    @FXML
    public void blackAndWhiteFilter() {
        currentPicture = images.filter(currentPicture, 2, false);
        imageView.setImage(currentPicture.getFilteredImage());
        noFilter.setImage(currentPicture.getImage());
        blueRedGreen.setImage(currentPicture.toBRG());
        blackAndWhite.setImage(currentPicture.toBlackAndWhite());
        sepia.setImage(currentPicture.toSepia());
        prewitt.setImage(currentPicture.toPrewitt());
    }

    @FXML
    public void sepiaFilter() {
        currentPicture = images.filter(currentPicture, 3, false);
        imageView.setImage(currentPicture.getFilteredImage());
        noFilter.setImage(currentPicture.getImage());
        blueRedGreen.setImage(currentPicture.toBRG());
        blackAndWhite.setImage(currentPicture.toBlackAndWhite());
        sepia.setImage(currentPicture.toSepia());
        prewitt.setImage(currentPicture.toPrewitt());
    }

    @FXML
    public void prewittFilter() {
        currentPicture = images.filter(currentPicture, 4, false);
        imageView.setImage(currentPicture.getFilteredImage());
        noFilter.setImage(currentPicture.getImage());
        blueRedGreen.setImage(currentPicture.toBRG());
        blackAndWhite.setImage(currentPicture.toBlackAndWhite());
        sepia.setImage(currentPicture.toSepia());
        prewitt.setImage(currentPicture.toPrewitt());
    }

    @FXML
    public void deleteSearchTag() {
        String deletedTag = itemsTag.getSelectionModel().getSelectedItem();
        tagList.remove(deletedTag);
        itemsTag.setItems(tagList);
        imagesWithTags();
    }

    @FXML
    public void deleteImage() {
        String deleteImage = items.getSelectionModel().getSelectedItem();
        imagesList.remove(deleteImage);
        int removed = 0;
        for (Picture picture : images.getPictures()) {
            if (picture.getFile().getName().equals(deleteImage)) {
                images.getPictures().remove(picture.getId());
                picture.getFile().delete();
                removed = picture.getId();
                break;
            }
        }
        for (int i = removed; i < images.getPictures().size(); i++) {
            images.getPictures().get(i).setId(images.getPictures().get(i).getId() - 1);
        }

        save();
        items.getSelectionModel().select(0);
        showImage();
    }

    @FXML
    public void deleteImageTag() {
        String deletedTag = imageTags.getSelectionModel().getSelectedItem();
        imageTagList.remove(deletedTag);
        for (String tag : images.getPictures().get(currentPicture.getId()).getTags()) {
            if (tag.equals(deletedTag)) {
                images.getPictures().get(currentPicture.getId()).getTags().remove(tag);
                break;
            }
        }
        save();
    }

    @FXML
    public void addPassword() {
        Alert createPassword = new Alert(Alert.AlertType.CONFIRMATION);
        ImageView secureIcon = new ImageView(Paths.get("src/main/resources/secure_icon.png").toFile().toURI().toString());
        secureIcon.setFitHeight(48);
        secureIcon.setFitWidth(48);
        stage = (Stage) createPassword.getDialogPane().getScene().getWindow();
        stage.getIcons().add(secureIcon.getImage());
        double width = createPassword.getDialogPane().getWidth();
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("Nouveau mot de passe");
        VBox parent = new VBox(newPassword);
        parent.setPadding(new Insets(10));
        createPassword.getDialogPane().setContent(parent);
        createPassword.getDialogPane().setGraphic(secureIcon);
        createPassword.getDialogPane().setMinWidth(width);
        createPassword.setTitle("Ajouter un mot de passe");
        createPassword.setHeaderText("Empêchez d'autres personnes d'accéder à votre image");
        createPassword.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    images.getPictures().get(currentPicture.getId()).encryptImage(newPassword.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        showImage();
    }

    @FXML
    public void decrypt() throws NoSuchAlgorithmException {
        images.getPictures().get(currentPicture.getId()).decryptImage(password.getText());
        showImage();
    }

    public void getCurrentImage() {
        imageTags.getItems().clear();
        String file = items.getSelectionModel().getSelectedItem();
        for (Picture picture : images.getPictures()) {
            if (picture.getFile().getName().equals(file)) {
                currentPicture = picture;
                imageTagList.removeAll();
                imageTagList.addAll(currentPicture.getTags());
                imageTags.setItems(imageTagList);
                break;
            }
        }
    }

    public void showImage() {
        addTag.setText(null);
        getCurrentImage();
        imageView.setImage(currentPicture.getFilteredImage());
        noFilter.setImage(currentPicture.getImage());
        blueRedGreen.setImage(currentPicture.toBRG());
        blackAndWhite.setImage(currentPicture.toBlackAndWhite());
        sepia.setImage(currentPicture.toSepia());
        prewitt.setImage(currentPicture.toPrewitt());
        if (currentPicture.isInvert()) {
            imageView.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            imageView.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
        rotation(currentPicture.getRotation());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        items.requestFocus();
    }

    public void loadImages() {
        JAXBContext jaxbContext = null;
        if (Paths.get("src/main/resources/save/dataPictures.xml").toFile().exists()) {
            try {
                jaxbContext = JAXBContext.newInstance(PictureDaoImpl.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                PictureDaoImpl pictureDao = (PictureDaoImpl) jaxbUnmarshaller.unmarshal(Paths.get("src/main/resources/save/dataPictures.xml").toFile());
                imagesList = FXCollections.observableArrayList();
                for (String fileName : pictureDao.getPicturesOrder()) {
                    imagesList.add(fileName);
                }
                for (String fileName : imagesList) {
                    File file = Paths.get("src/main/resources/images/" + fileName).toFile();
                    Picture img = new Picture(new Image(file.toURI().toString()), file, images.getPictures().size());
                    images.addPicture(img);
                }
                for (Picture picture : images.getPictures()) {
                    Picture pictureTemp = pictureDao.getPictures().get(picture.getId());
                    picture.setFile(pictureTemp.getFile());
                    picture.setTags(pictureTemp.getTags());
                    picture.setChanges(pictureTemp.getChanges());
                    for (int i = 0; i < picture.getChanges().size(); i++) {
                        images.filter(picture, picture.getChanges().get(i), true);
                    }
                    picture.setRotation(pictureTemp.getRotation());
                    picture.setId(pictureTemp.getId());
                    picture.setInvert(pictureTemp.isInvert());
                }

            } catch (JAXBException e) {
                e.printStackTrace();
            }
        } else {
            File directory = Paths.get("src/main/resources/images/").toFile();
            for (File file : directory.listFiles()) {
                Picture img = new Picture(new Image(file.toURI().toString()), file, images.getPictures().size());
                images.addPicture(img);
                imagesList.add(img.getFile().getName());
            }
        }
        items.setItems(imagesList);
    }

    public void imagesWithTags() {
        ObservableList<String> matchingList = FXCollections.observableArrayList();

        for (Picture picture : images.getPictures()) {
            if (tagList.stream().allMatch(picture.getTags()::contains)) {
                matchingList.add(picture.getFile().getName());
            }
        }
        imagesList = matchingList;
        items.setItems(imagesList);
    }

    public void rotation(int x) {
        currentPicture.setRotation(x);
        imageView.setRotate(currentPicture.getRotation());
    }

    @FXML
    public void save() {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(PictureDaoImpl.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            PictureDaoImpl pictureDao = new PictureDaoImpl();
            //images.getPictures().sort(Picture::compareTo);
            pictureDao.setPictures(images.getPictures());
            ArrayList<String> orderList = new ArrayList<>();
            for (String fileName : imagesList) {
                orderList.add(fileName);
            }
            pictureDao.setPicturesOrder(orderList);
            jaxbMarshaller.marshal(pictureDao, Paths.get("src/main/resources/save/dataPictures.xml").toFile());

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

}