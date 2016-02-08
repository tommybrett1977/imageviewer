package acdsee.sorting.comparator.file;

import java.util.Comparator;
import acdsee.ui.components.thumbnail.FileThumbnail;
import org.apache.commons.io.comparator.NameFileComparator;

/**
 *
 * @author Tommy Brettschneider
 */
public class FileNameComparator implements Comparator<FileThumbnail> {

    @Override
    public int compare(FileThumbnail source, FileThumbnail target) {
        return NameFileComparator.NAME_COMPARATOR.compare(source.getFile(), target.getFile());
    }
}