package nl.alexeyu.photomate.ui;

import static nl.alexeyu.photomate.ui.UiConstants.BORDER_WIDTH;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.*;

import nl.alexeyu.photomate.api.AbstractPhoto;
import nl.alexeyu.photomate.model.DefaultPhotoMetaData;
import nl.alexeyu.photomate.model.PhotoMetaData;
import nl.alexeyu.photomate.model.PhotoProperty;
import nl.alexeyu.photomate.service.PhotoObserver;

public abstract class AbstractPhotoMetaDataPanel<P extends AbstractPhoto> extends JPanel
        implements PropertyChangeListener, PhotoObserver<P> {

    protected HintedTextField captionEditor;

    protected HintedTextField descriptionEditor;

    protected JList<String> keywordList = new JList<>(new DefaultListModel<>());

    protected P photo;

    public AbstractPhotoMetaDataPanel() {
        super(new BorderLayout(BORDER_WIDTH, BORDER_WIDTH));
        var editorPanel = new JPanel();
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));

        captionEditor = HintedTextField.textArea("Caption", PhotoProperty.CAPTION.propertyName());
        editorPanel.add(captionEditor);
        descriptionEditor = HintedTextField.textArea("Description", PhotoProperty.DESCRIPTION.propertyName());
        editorPanel.add(descriptionEditor);

        add(editorPanel, BorderLayout.NORTH);
        add(new JScrollPane(keywordList), BorderLayout.CENTER);

        setPreferredSize(UiConstants.PREVIEW_SIZE);
    }

    public final void setPhoto(P photo) {
        if (this.photo != null) {
            this.photo.removePropertyChangeListener(this);
        }
        this.photo = photo;
        updateComponentsWithPhotoMetaData();
        if (this.photo != null) {
            this.photo.addPropertyChangeListener(this);
        }
    }

    private void updateComponentsWithPhotoMetaData() {
        var listModel = new DefaultListModel<String>();
        if (this.photo != null) {
            PhotoMetaData metaData = new DefaultPhotoMetaData(photo.metaData());
            var keywords = new ArrayList<>(metaData.keywords());
            Collections.sort(keywords);
            listModel.addAll(keywords);
            SwingUtilities.invokeLater(() -> {
                captionEditor.setText(metaData.caption());
                descriptionEditor.setText(metaData.description());
                keywordList.setModel(listModel);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                captionEditor.setText("");
                descriptionEditor.setText("");
                keywordList.setModel(listModel);
            });
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (AbstractPhoto.METADATA_PROPERTY.equals(e.getPropertyName()) && e.getSource() == photo) {
            updateComponentsWithPhotoMetaData();
        } else {
            firePropertyChange(e.getPropertyName(), null, e.getNewValue());
        }
    }

    @Override
    public void photoSelected(P photo) {
        setPhoto(photo);
    }

}
