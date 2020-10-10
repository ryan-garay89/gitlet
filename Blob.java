package gitlet;
import java.io.File;
import java.io.Serializable;

/** A representation of a file within a commit.
 * @author ryangaray
 * */
public class Blob implements Serializable {

    /** Constructs a new Blob whose file is FILE. */
    public Blob(File file) {
        _file = file;
        contents = Utils.readContentsAsString(file);
        fileID = Utils.sha1((Object) Utils.readContents(file));
        _ID = Utils.sha1((Object) Utils.serialize(this));
        File idFile = Utils.join(Main.OBJECTS, _ID);
        Utils.writeObject(idFile, this);
    }

    /** Returns the Blob stored as a file in OBJECTS with name NAME. */
    public static Blob fromFileObj(String name) {
        Blob blob = null;
        File file = new File(Main.OBJECTS, name);
        if (file.exists()) {
            blob = Utils.readObject(file, Blob.class);
        }
        if (blob != null) {
            return blob;
        } else {
            throw Utils.error("Blob " + name + " could not be found in stage");
        }
    }

    /** Stages this blob for addition. */
    public void saveBlob() {
        File file = new File(Main.STAGE, _file.getName());
        Utils.writeObject(file, this);
    }

    /** Stages this blob for removal. */
    public void deleteBlob() {
        File file = new File(Main.REMOVE, _file.getName());
        Utils.writeObject(file, this);
    }

    /** Returns the ID of this blob. */
    public String getID() {
        return _ID;
    }

    /** Returns the file this blob represents. */
    public File getFile() {
        return _file;
    }

    /** Sets the file to FILE. */
    public void setFile(File file) {
        _file = file;
        fileID = Utils.sha1((Object) Utils.readContents(file));
        contents = Utils.readContentsAsString(file);
    }

    /** Returns the contents of this Blob's file. */
    public String getContents() {
        return contents;
    }

    /** Returns iff this Blob's file is modified. */
    public boolean modified() {
        return !Utils.readContentsAsString(_file).equals(contents);
    }

    /** Returns iff this blob's file is deleted. */
    public boolean deleted() {
        return !_file.exists();
    }

    /** Delete's this Blob's file. */
    public void destroyBlob() {
        if (_file.exists()) {
            _file.delete();
        }
    }

    /** The file of this blob. */
    private File _file;
    /** ID of the file. */
    private String fileID;
    /** The SHA-1 ID of this blob. */
    private String _ID;
    /** The contents of this Blob's file. */
    private String contents;
}
