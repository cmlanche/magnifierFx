package sample;

import com.cmlanche.magnifier.Magnifier;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private VBox container;
    @FXML
    private ImageView imageView;

    private Magnifier magnifier;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        magnifier = new Magnifier();
        magnifier.setActive(true);
        magnifier.setRadius(60);
        magnifier.setFrameWidth(4);
        magnifier.setScaleFactor(2);
        magnifier.setScopeLineWidth(1.5);
        magnifier.setScopeLinesVisible(true);
        magnifier.setContent(imageView);

        container.getChildren().add(magnifier);
    }
}
