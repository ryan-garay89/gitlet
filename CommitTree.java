package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.Arrays;

/** Tree representation of commits.
 * @author ryangaray
 */
public class CommitTree implements Serializable {

    /** Construct the first commit in a tree which has no
     * parent and inherits no files. Has log MSG. */
    public CommitTree(String msg) {
        _head = new CommitNode(msg);
        _headName = "master";
        _branches.put("master", _head.getID());
        _branchHistory.put(_headName, new LinkedList<>());
    }

    /** Constructs a commit with log MSG whose parent is
     * the previous head commit. */
    public void commit(String msg) {
        _branchHistory.get(_headName).add(_head.getID());
        _head = new CommitNode(msg, _head.getID());
        if (_headName.equals("master")) {
            _branches.replace("master", _head.getID());
        } else {
            if (_branches.containsKey(_headName)) {
                _branches.replace(_headName, _head.getID());
            } else {
                _branches.put(_headName, _head.getID());
            }
        }
        clear();
    }

    /** Constructor for a merged commit whose parent is
     * the previous head and whose SECONDPARENT is the
     * merged in parent. Has log MSG. */
    public void commit(String msg, CommitNode secondParent) {
        _branchHistory.get(_headName).add(_head.getID());
        _branchHistory.get(_headName).add(secondParent.getID());
        _head = new CommitNode(msg, _head.getID(), secondParent.getID());
        if (_headName.equals("master")) {
            _branches.replace("master", _head.getID());
        } else {
            if (_branches.containsKey(_headName)) {
                _branches.replace(_headName, _head.getID());
            } else {
                _branches.put(_headName, _head.getID());
            }
        }
        clear();
    }

    /** Clears the stage. */
    public void clear() {
        File[] files = Main.STAGE.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        File[] removeFiles = Main.REMOVE.listFiles();
        if (removeFiles != null) {
            for (File file : removeFiles) {
                file.delete();
            }
        }
    }

    /** Creates a new branch with name NAME. */
    public void branch(String name) {
        if (_branches.containsKey(name)) {
            System.out.println("A branch with that "
                    + "name already exists.");
            System.exit(0);
        }
        _branches.put(name, _head.getID());
        _branchHistory.put(name, new LinkedList<>());
        _branchHistory.get(name).addAll(_branchHistory.get(_headName));
    }

    /** Prints information about all branches. */
    public void status() {
        String branches = "";
        String[] branchArray = _branches.keySet().toArray(String[]::new);
        Arrays.sort(branchArray);
        for (String branch : branchArray) {
            if (_headName.equals(branch)) {
                branches += "*" + branch + "\n";
            } else {
                branches += branch + "\n";
            }
        }
        String stagedForAdd = "";
        String stagedForRm = "";
        String untracked = "";
        List<String> stageAdd = Utils.plainFilenamesIn(Main.STAGE);
        List<String> stageRemove = Utils.plainFilenamesIn(Main.REMOVE);
        if (stageAdd != null) {
            for (String filename : stageAdd) {
                Blob blob = Utils.readObject(new File(Main.STAGE,
                        filename), Blob.class);
                stagedForAdd += filename + "\n";
            }
        }
        if (stageRemove != null) {
            for (String filename : stageRemove) {
                stagedForRm += filename + "\n";
            }
        }
        List<String> workingFiles = Utils.plainFilenamesIn(Main.CWD);
        if (workingFiles != null) {
            for (String filename : workingFiles) {
                File stage = new File(Main.STAGE, filename);
                File stageRm = new File(Main.REMOVE, filename);
                if (!stage.exists() && _head.getFiles() != null
                        && !_head.getFiles().containsKey(filename)) {
                    untracked += filename + "\n";
                } else if (stageRm.exists()) {
                    untracked += filename + "\n";
                }
            }
        }
        System.out.println("=== Branches ===\n" + branches
                + "\n=== Staged Files ===\n"
                + stagedForAdd + "\n=== Removed Files ===\n"
                + stagedForRm
                + "\n=== Modifications Not Staged For Commit ===\n"
                + "\n=== Untracked Files ===\n" + untracked);
    }

    /** Prints logs of all commits ever made. */
    public void globalLog() {
        File[] files = Main.NODES.listFiles();
        if (files != null) {
            for (File file : files) {
                CommitNode commit = Utils.readObject(file,
                        CommitNode.class);
                System.out.println(commit.toString());
            }
        }
    }

    /** Removes file with NAME. */
    public void removeFile(String name) {
        File file = new File(Main.STAGE, name);
        boolean staged = false;
        if (file.exists()) {
            file.delete();
            staged = true;
        }
        if (_head.getFiles() != null && _head.containsFile(name)) {
            File stagerm = new File(Main.REMOVE, file.getName());
            try {
                stagerm.createNewFile();
            } catch (IOException e) {
                throw Utils.error(e.getMessage());
            }
            Blob blob = Utils.readObject(new File(Main.OBJECTS,
                    _head.getFiles().get(name)), Blob.class);
            blob.deleteBlob();
            blob.destroyBlob();
        } else if (!staged) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** Removes branch from _branches with name NAME. */
    public void removeBranch(String name) {
        if (_headName.equals(name)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else if (!_branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        _branches.remove(name);
        _branchHistory.remove(name);
    }

    /** Checks out a Blob BLOB. */
    public void checkout(Blob blob) {
        if (_head.getFiles() != null && _head.getFiles().containsKey(
                blob.getFile().getName())) {
            Utils.writeContents(blob.getFile(), blob.getContents());
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /** Checks out Blob BLOB found in COMMIT. */
    public void checkout(String commit, Blob blob) {
        if (commit.length() < _shortIDLength) {
            commit = findCommitID(commit);
        }
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        File nodeFile = new File(Main.NODES, commit);
        if (!nodeFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        CommitNode node = Utils.readObject(nodeFile, CommitNode.class);
        if (node.getFiles() != null && node.getFiles().containsKey(
                blob.getFile().getName())) {
            Utils.writeContents(blob.getFile(), blob.getContents());
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /** Checks out Branch BRANCH with given name. */
    public void checkout(String branch) {
        if (!_branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        } else if (branch.equals(_headName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        CommitNode head = Utils.readObject(new File(Main.NODES,
                _branches.get(branch)), CommitNode.class);
        for (String filename : Objects.requireNonNull(
                Utils.plainFilenamesIn(Main.CWD))) {
            if (!_head.containsFile(filename) && head.containsFile(filename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        head.toWorkingDir();
        _headName = branch;
        if (_head.getFiles() != null) {
            for (String filename : _head.getFiles().keySet()) {
                if (!head.containsFile(filename)) {
                    Blob blob = Blob.fromFileObj(
                            _head.getFiles().get(filename));
                    blob.destroyBlob();
                }
            }
        }
        _head = head;
        clear();

    }

    /** Returns the commitID from a SHORTID given. */
    public String findCommitID(String shortID) {
        List<String> files = Utils.plainFilenamesIn(Main.NODES);
        boolean passed = false;
        boolean found = false;
        if (files != null) {
            for (String filename : files) {
                if (found && passed) {
                    break;
                } else {
                    if (filename.contains(shortID)) {
                        return filename;
                    } else if (!found && filename.charAt(0)
                            == shortID.charAt(0)) {
                        found = true;
                    } else if (found && !(filename.charAt(0)
                            == shortID.charAt(0))) {
                        passed = true;
                    }
                }
            }
        } else {
            System.out.println("Nodes directory does not exist.");
            System.exit(0);
        }
        return null;
    }

    /** Merges the current head into BRANCH. */
    public void merge(String branch) {
        if (!_branches.containsKey(branch)) {
            System.out.println("A branch with that name does "
                    + "not exist.");
            System.exit(0);
        }
        CommitNode branchNode = Utils.readObject(new File(Main.NODES,
                _branches.get(branch)), CommitNode.class);
        File[] stage = Main.STAGE.listFiles(); File[] remove
                = Main.REMOVE.listFiles();
        if (stage != null && remove != null) {
            if (stage.length > 0 || remove.length > 0) {
                System.out.println("You have uncommitted changes.");
                System.exit(0);
            }
        }
        if (branchNode.getID().equals(_head.getID())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        for (String filename : Objects.requireNonNull(
                Utils.plainFilenamesIn(Main.CWD))) {
            if (!_head.containsFile(filename)
                    && branchNode.containsFile(filename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        findAncestor(branch, 0, _head);
        Integer min = Collections.min(_potentialAncestors.keySet());
        CommitNode ancestor = _potentialAncestors.get(min);
        _potentialAncestors.clear();
        if (ancestor.getID().equals(branchNode.getID())
                || _branchHistory.get(_headName).contains(branchNode.getID())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            System.exit(0);
        }
        if (_head.getID().equals(ancestor.getID())
                || branchNode.getParent().equals(_head.getID())) {
            checkout(branch);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        checkBranchNode(branchNode, ancestor);
        checkAncestorNode(ancestor, branchNode);
        if (_head.getFiles() != null) {
            checkHeadNode(branchNode, ancestor);
        }
        commit("Merged " + branch + " into "
                + _headName + ".", branchNode);
        if (_conflicted) {
            System.out.println("Encountered a merge conflict.");
        }
        _conflicted = false;
    }

    /** A helper function for merge which checks
     * the head against BRANCHNODE and ANCESTOR. */
    private void checkHeadNode(CommitNode branchNode, CommitNode ancestor) {
        for (String fil : _head.getFiles().keySet()) {
            Blob headBlob = Blob.fromFileObj(_head.getFiles().get(fil));
            if (((branchNode.getFiles() != null
                    && !branchNode.getFiles().containsKey(fil))
                    || branchNode.getFiles() == null)
                    && (ancestor.getFiles() == null
                    || (ancestor.getFiles() != null
                    && !ancestor.getFiles().containsKey(fil)))) {
                headBlob.saveBlob();
            }
            if (branchNode.getFiles() != null
                    && branchNode.getFiles().containsKey(fil)
                    && ancestor.getFiles() != null) {
                Blob branchBlob = Blob.fromFileObj(
                                branchNode.getFiles().get(fil));
                Blob ancestorBlob =
                        Blob.fromFileObj(
                                ancestor.getFiles().get(fil));
                if (ancestor.getFiles() != null
                        && ancestor.getFiles().containsKey(fil)) {
                    if (!headBlob.getContents().equals(
                            ancestorBlob.getContents())
                            && branchBlob.getContents().equals(
                            ancestorBlob.getContents())) {
                        headBlob.saveBlob();
                    }
                }
            }
            if (branchNode.getFiles() != null
                    && branchNode.getFiles().containsKey(fil)) {
                Blob branchBlob
                        = Blob.fromFileObj(branchNode.getFiles().get(fil));
                if (!branchBlob.getContents().equals(headBlob.getContents())
                        && !_checkedOut) {
                    Utils.writeContents(
                            headBlob.getFile(), "<<<<<<< HEAD\n"
                                    + headBlob.getContents() + "=======\n"
                                    + branchBlob.getContents() + ">>>>>>>\n");

                    headBlob.saveBlob();
                    _conflicted = true;
                }
            } else {
                if (ancestor.getFiles() != null
                        && ancestor.getFiles().containsKey(fil)) {
                    Blob ancestorBlob
                            = Blob.fromFileObj(ancestor.getFiles().get(fil));
                    if (!ancestorBlob.getContents().equals(
                            headBlob.getContents())) {
                        Utils.writeContents(headBlob.getFile(),
                                "<<<<<<< HEAD\n" + headBlob.getContents()
                                        + "=======\n" + ">>>>>>>\n");
                        headBlob.saveBlob();
                        _conflicted = true;
                    }
                }
            }
        }
    }

    /** A helper function for merge which checks
     * he ANCESTOR against BRANCHNODE and _head. */
    private void checkAncestorNode(CommitNode ancestor,
                                   CommitNode branchNode) {
        if (ancestor.getFiles() != null) {
            for (String filename : ancestor.getFiles().keySet()) {
                Blob ancestorBlob
                        = Blob.fromFileObj(ancestor.getFiles().get(filename));
                if (_head.getFiles() != null
                        && _head.getFiles().containsKey(filename)) {
                    Blob headBlob = Blob.fromFileObj(
                            _head.getFiles().get(filename));
                    if (((branchNode.getFiles() != null
                            && !branchNode.getFiles().containsKey(filename))
                            || branchNode.getFiles() == null)
                            && headBlob.getContents().equals(
                                    ancestorBlob.getContents())) {
                        Utils.restrictedDelete(
                                new File(headBlob.getFile().getName()));
                        headBlob.deleteBlob();
                        _head.removeFile(filename);
                    }
                }
                if (branchNode.getFiles() != null
                        && branchNode.getFiles().containsKey(filename)
                        && !_head.getFiles().containsKey(filename)) {
                    Blob branchBlob
                            = Blob.fromFileObj(
                                    branchNode.getFiles().get(filename));
                    if (branchBlob.getContents().equals(
                            ancestorBlob.getContents())) {
                        branchBlob.deleteBlob();
                    }
                }
            }
        }
    }

    /** A helper function for merge which checks the
     * BRANCHNODE against head and ANCESTOR. */
    private void checkBranchNode(CommitNode branchNode,
                                 CommitNode ancestor) {
        if (branchNode.getFiles() != null) {
            for (String fil : branchNode.getFiles().keySet()) {
                Blob branchBlob = Blob.fromFileObj(
                        branchNode.getFiles().get(fil));
                if (ancestor.getFiles() != null
                        && ancestor.getFiles().containsKey(fil)
                        && _head.getFiles()
                        != null && _head.getFiles().containsKey(fil)) {
                    Blob headBlob = Blob.fromFileObj(_head.getFiles().get(fil));
                    Blob ancestorB
                            = Blob.fromFileObj(ancestor.getFiles().get(fil));
                    if (headBlob.getContents().equals(ancestorB.getContents())
                        && !branchBlob.getContents().equals(
                                ancestorB.getContents())) {
                        checkout(branchNode.getID(), branchBlob);
                        branchBlob.saveBlob();
                        _checkedOut = true;
                    }
                    if (headBlob.getContents().equals(
                            branchBlob.getContents())
                            && !headBlob.getContents().equals(
                                    ancestorB.getContents())) {
                        headBlob.saveBlob();
                    }
                }
                if (((ancestor.getFiles() != null
                        && !ancestor.getFiles().containsKey(fil))
                        || ancestor.getFiles() == null)
                        && (_head.getFiles() == null || (_head.getFiles()
                        != null && !_head.getFiles().containsKey(fil)))) {
                    checkout(branchNode.getID(), branchBlob);
                    branchBlob.saveBlob();
                }
                if (ancestor.getFiles() != null
                        && ancestor.getFiles().containsKey(fil)) {
                    Blob ancestorBlob = Blob.fromFileObj(
                            ancestor.getFiles().get(fil));
                    if (((_head.getFiles() != null
                            && !_head.getFiles().containsKey(fil))
                            || _head.getFiles() == null)
                            && !ancestorBlob.getContents().equals(
                                    branchBlob.getContents())) {
                        Utils.writeContents(ancestorBlob.getFile(),
                                "<<<<<<< HEAD\n" + "\n"
                                + "=======\n" + branchBlob.getContents()
                                        + ">>>>>>>\n");
                        ancestorBlob.saveBlob();
                        _conflicted = true;
                    }
                }
            }
        }
    }

    /** Finds the ancestor of the current head
     * and given BRANCHNAME for merge.
     * Starts at START and tracks PATHLENGTH. */
    private void findAncestor(String branchName, int pathLength,
                              CommitNode start) {
        CommitNode currHead = start;
        while (currHead != null) {
            if (currHead.isMerged()) {
                CommitNode secondParent = Utils.readObject(
                        new File(Main.NODES,
                        currHead.secondParent()), CommitNode.class);
                if (_branchHistory.get(branchName).contains(
                        currHead.secondParent())) {
                    _potentialAncestors.put(pathLength, secondParent);
                    return;
                } else {
                    findAncestor(branchName, pathLength + 1, secondParent);
                }
            }
            if (_branchHistory.get(branchName).contains(
                    currHead.getID())) {
                _potentialAncestors.put(pathLength, Utils.readObject(
                        new File(Main.NODES, currHead.getID()),
                        CommitNode.class));
                return;
            }
            if (currHead.getParent() == null) {
                break;
            }
            currHead = Utils.readObject(new File(Main.NODES,
                    currHead.getParent()), CommitNode.class);
            pathLength++;
        }
    }

    /** Finds the commit with given MSG. */
    public void find(String msg) {
        boolean found = false;
        File[] files = Main.NODES.listFiles();
        if (files != null) {
            for (File file : files) {
                CommitNode curr = Utils.readObject(file,
                        CommitNode.class);
                if (curr.getLog().equals(msg)) {
                    System.out.println(curr.getID());
                    found = true;
                }
            }
            if (!found) {
                System.out.println("Found no commit with that "
                        + "message.");
                System.exit(0);
            }
        } else {
            System.out.println("Found no commit with that "
                    + "message.");
            System.exit(0);
        }
    }

    /** Returns metadata for each commit. */
    public void log() {
        CommitNode curr = _head;
        while (curr != null) {
            System.out.println(curr.toString());
            if (curr.getParent() == null) {
                break;
            } else {
                curr = Utils.readObject(new File(Main.NODES,
                    curr.getParent()), CommitNode.class);
            }
        }
    }

    /** Resets to the COMMIT with given ID. */
    public void reset(String commit) {
        if (commit.length() < _shortIDLength) {
            commit = findCommitID(commit);
        }
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        File file = new File(Main.NODES, commit);
        if (!file.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        CommitNode commitNode = Utils.readObject(file, CommitNode.class);
        for (String filename : Objects.requireNonNull(
                Utils.plainFilenamesIn(Main.CWD))) {
            if (!_head.containsFile(filename)
                    && commitNode.containsFile(filename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        if (commitNode.getFiles() != null) {
            for (String filename : commitNode.getFiles().keySet()) {
                String blobID = commitNode.getFiles().get(filename);
                Blob blob = Blob.fromFileObj(blobID);
                checkout(commitNode.getID(), blob);
            }
        }
        if (_head.getFiles() != null) {
            for (String filename : _head.getFiles().keySet()) {
                if (!commitNode.containsFile(filename)) {
                    Blob blob = Blob.fromFileObj(
                            _head.getFiles().get(filename));
                    blob.destroyBlob();
                }
            }
        }
        _head = commitNode;
        _branches.replace(_headName, _head.getID());
        clear();
    }

    /** Returns _head. */
    public CommitNode getHead() {
        return _head;
    }

    /** Returns branches. */
    public HashMap<String, String> getBranches() {
        return _branches;
    }

    /** Returns _headName. */
    public String currentBranch() {
        return _headName;
    }

    /** The current head of this tree. */
    private CommitNode _head;
    /**The current name of the active branch. */
    private String _headName;
    /** Mapping of branches to commits. */
    private HashMap<String, String> _branches = new HashMap<>();
    /** A mapping of the history of each branch.
     * Keys are Branch names & Values are Commit Node IDs.
     */
    private HashMap<String, LinkedList<String>>
            _branchHistory = new HashMap<>();
    /** Mapping of potential ancestors for merge. */
    private HashMap<Integer, CommitNode> _potentialAncestors = new HashMap<>();
    /** Denotes if a merge is in conflict. */
    private boolean _conflicted;
    /** Check for whether or not branch has been checked out during merge. */
    private boolean _checkedOut;
    /** Short ID length. */
    private final int _shortIDLength = 40;

}
