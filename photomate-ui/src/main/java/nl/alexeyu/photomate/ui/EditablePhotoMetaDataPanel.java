package nl.alexeyu.photomate.ui;

import static java.util.Arrays.asList;
import static nl.alexeyu.photomate.model.PhotoProperty.CAPTION;
import static nl.alexeyu.photomate.model.PhotoProperty.DESCRIPTION;
import static nl.alexeyu.photomate.model.PhotoProperty.KEYWORDS;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.alexeyu.photomate.api.editable.EditablePhoto;

public class EditablePhotoMetaDataPanel extends AbstractPhotoMetaDataPanel<EditablePhoto> {

    private static final String NEW_KEYWORD_PROPERTY = "newKeyword";

    private static final Logger logger = LogManager.getLogger();

    private HintedTextField keywordToAddField;

    public EditablePhotoMetaDataPanel() {
        captionEditor.addPropertyChangeListener(CAPTION.propertyName(), this);
        descriptionEditor.addPropertyChangeListener(DESCRIPTION.propertyName(), this);

        keywordList.addKeyListener(new KeywordRemover());
        keywordList.addPropertyChangeListener(KEYWORDS.propertyName(), this);

        keywordToAddField = HintedTextField.textField("Keyword to add", NEW_KEYWORD_PROPERTY);
        add(keywordToAddField, BorderLayout.SOUTH);
        keywordToAddField.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (NEW_KEYWORD_PROPERTY.equals(e.getPropertyName())) {
            addKeywords(asList(e.getNewValue().toString()));
            keywordToAddField.setText("");
        } else {
            super.propertyChange(e);
        }
    }

    private void removeKeywords(List<String> keywords) {
        if (photo != null && keywords.size() > 0) {
            var reducedKeywords = new LinkedHashSet<>(photo.keywords());
            reducedKeywords.removeAll(keywords);
            firePropertyChange(KEYWORDS.propertyName(), photo.keywords(), reducedKeywords);
        }
    }

    private void addKeywords(List<String> keywords) {
        if (photo != null) {
            var extendedKeywords = new LinkedHashSet<>(photo.keywords());
            extendedKeywords.addAll(keywords);
            firePropertyChange(KEYWORDS.propertyName(), photo.keywords(), extendedKeywords);
        }
    }

    public DropTarget getDropTarget() {
        return new DropTarget(keywordList, new KeywordDropTarget());
    }

    private final class KeywordRemover extends KeyAdapter {
        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_DELETE) {
                removeKeywords(keywordList.getSelectedValuesList());
            }
        }
    }

    private final class KeywordDropTarget extends DropTargetAdapter {

        @Override
        public void drop(DropTargetDropEvent dtde) {
            try {
                var dataFlavor = new DataFlavor("text/plain; class=java.lang.String");
                var draggedValue = dtde.getTransferable().getTransferData(dataFlavor).toString();
                logger.debug("Dragged keywords: {}", draggedValue);
                var keywords = toList(draggedValue);
                addKeywords(keywords);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private List<String> toList(String keywords) {
            if (keywords.contains(System.lineSeparator())) {
                return asList(keywords.split(System.lineSeparator()));
            }
            // Windows can get crazy and disrespect the line separator property.
            if (keywords.contains("\n")) {
                return asList(keywords.split("\n"));
            }
            return List.of(keywords);
        }
    }
}
