package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/** A node in a commit tree representing a commit.
 * @author ryangaray
 * */
public class CommitNode implements Serializable,
        Dumpable {

    /** Constructs a new CommitNode with message MSG, and a
     * mapping of Filenames to SHA-1 IDs
     * in BLOBS. */
    public CommitNode(String msg) {
        _parent = null;
        _log = msg;
        Date date = new Date();
        _timestamp = date.toString().substring(0, _END)
                + date.toString().substring(_LONGEND) + " -0800";
        _ID = Utils.sha1((Object) Utils.serialize(this));
        saveNode();
    }

    /** Constructs a new CommitNode with message MSG, whose
     * parent is given by PARENT which
     * is a SHA-1 ID of another CommitNode.
     * This node retains the blobs of its parent. */
    public CommitNode(String msg, String parent) {
        _parent = parent;
        buildBlobs();
        _log = msg;
        Date date = new Date();
        _timestamp = date.toString().substring(0, _END)
                + date.toString().substring(_LONGEND) + " -0800";
        _ID = Utils.sha1((Object) Utils.serialize(this));
        saveNode();
    }

    /** Constructs a new CommitNode when a second parent is given.
     * Where message is MSG, first parent is FIRSTPARENT and second parent
     * is SECONDPARENT.*/
    public CommitNode(String msg, String firstParent, String secondParent) {
        _parent = firstParent;
        _secondParent = secondParent;
        _merged = true;
        _log = msg;
        buildBlobs();
        Date date = new Date();
        _timestamp = date.toString().substring(0, _END)
                + date.toString().substring(_LONGEND) + " -0800";
        _ID = Utils.sha1((Object) Utils.serialize(this));
        saveNode();
    }

    /** Builds the _files variable of this commit. Adds _files
     * from its _parent
     * and removes them if they are staged for removal.
     * Modifies them if they
     * are of a different version than their parent's.
     */
    private void buildBlobs() {
        File parent = new File(Main.NODES, _parent);
        if (parent.exists()) {
            CommitNode parentCommit = Utils.readObject(parent,
                    CommitNode.class);
            if (parentCommit.getFiles() != null) {
                _files.putAll(parentCommit.getFiles());
            }
            if (_merged) {
                File secondParentFile = new File(Main.NODES,
                        _secondParent);
                if (secondParentFile.exists()) {
                    CommitNode secondParent = Utils.readObject(secondParentFile,
                            CommitNode.class);
                    if (secondParent.getFiles() != null) {
                        _files.putAll(secondParent.getFiles());
                    }
                } else {
                    throw Utils.error("Second parent " + _secondParent
                            + " could not be found in NODES.");
                }
            }
        } else {
            throw Utils.error("Parent " + _parent + " could not "
                    + "be found in NODES.");
        }
        File[] files = Main.REMOVE.listFiles();
        if (files != null) {
            for (File file : files) {
                _files.remove(file.getName());
            }
        }
        files = Main.STAGE.listFiles();
        if (files != null) {
            for (File file : files) {
                Blob stageBlob = Utils.readObject(file, Blob.class);
                String fileName = stageBlob.getFile().getName();
                if (_files.containsKey(fileName)) {
                    Blob fileBlob = Blob.fromFileObj(_files.get(fileName));
                    if (!fileBlob.getContents().equals(
                            stageBlob.getContents())) {
                        _files.remove(fileName);
                    }
                }
                _files.put(fileName, stageBlob.getID());
            }
        }
    }

    /** Saves this CommitNode to NODES. */
    public void saveNode() {
        File file = Utils.join(Main.NODES, _ID);
        Utils.writeObject(file, this);
    }

    /** Removes file with name NAME in _files. */
    public void removeFile(String name) {
        if (_files.containsKey(name)) {
            _files.remove(name);
            update();
        } else {
            throw Utils.error("Blob " + name + " does not exist in commit.");
        }
    }

    /** Sends this commits files to the working directory. */
    public void toWorkingDir() {
        for (String filename : _files.keySet()) {
            File file = new File(filename);
            Blob blob = Blob.fromFileObj(_files.get(filename));
            Utils.writeContents(file, blob.getContents());
        }
    }

    /** Returns iff this commit contains file with NAME. */
    public boolean containsFile(String name) {
        return _files.containsKey(name);
    }

    /** Updates the ID of this commit. */
    private void update() {
        File delete = new File(Main.NODES, _ID);
        delete.delete();
        _ID = Utils.sha1((Object) Utils.serialize(this));
        File file = Utils.join(Main.NODES, _ID);
        Utils.writeObject(file, this);
    }

    /** Returns the timestamp of when this node was created. */
    public String timestamp() {
        return _timestamp;
    }

    /** Returns the log message of this commit. */
    public String getLog() {
        return _log;
    }

    /** Returns the parent of this commit. */
    public String getParent() {
        if (_parent == null) {
            return null;
        }
        return _parent;
    }

    /** Returns the SHA-1 ID of this commit. */
    public String getID() {
        return _ID;
    }

    /** Returns iff this node is merged. */
    public boolean isMerged() {
        return _merged;
    }

    /** Returns the second parent of this node if _merged. */
    public String secondParent() {
        return _secondParent;
    }

    /** Returns the files tracked by this CommitNode. */
    public Map<String, String> getFiles() {
        if (_parent == null) {
            return null;
        }
        return _files;
    }

    @Override
    public String toString() {
        if (isMerged()) {
            return "===\n" + "commit " + getID() + "\n"
                    + "Merge: "
                    + getParent().substring(0, 7) + " "
                    + secondParent().substring(0, 7)
                    + "\n" + "Date: " + timestamp() + "\n"
                    + getLog() + "\n";
        } else {
            return "===\n" + "commit " + getID() + "\n"
                    + "Date: "
                    + timestamp() + "\n" + getLog() + "\n";
        }
    }

    /** Dumps this commit using Dumpable. */
    @Override
    public void dump() {
        System.out.printf("size: %d%nmapping: %s%n", _files.size(), _files);
    }

    /** The log message of this commit. */
    private String _log;
    /** The time when this commit was created. */
    private String _timestamp;
    /** The SHA-1 ID of this commit. */
    private String _ID;
    /** The mapping of filenames to SHA-1 IDs contained in this commit. */
    private HashMap<String, String> _files = new HashMap<>();
    /** The parent of this commit. */
    private String _parent;
    /** Any parent created during a merge with this commit. */
    private String _secondParent;
    /** If this commit has been merged. */
    private boolean _merged;
    /** Length of first substring. */
    private final int _END = 20;
    /** Length of the second substring. */
    private final int _LONGEND = 24;

}
