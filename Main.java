package gitlet;

import java.io.File;
import java.util.Objects;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Ryan Garay
 */
public class Main {

    /** String of the working directory. */
    static final String WORKINGDIRECTORY = new File("").getAbsolutePath();

    /** Pointer to the working directory. */
    static final File CWD = new File(WORKINGDIRECTORY);

    /** Pointer to the .gitlet directory. */
    static final File GITLET = new File(".gitlet");

    /** Pointer to the stage directory. */
    static final File STAGE = Utils.join(GITLET, "stage");

    /** Pointer to the objects directory. */
    static final File OBJECTS = Utils.join(GITLET, "objects");

    /** Pointer to the nodes directory. */
    static final File NODES = Utils.join(GITLET, "commits");

    /** Pointer to the stage removal directory. */
    static final File REMOVE = Utils.join(GITLET, "remove");

    /** Pointer to the head directory. */
    static final File HEAD = Utils.join(GITLET, "head");

    /** Value for Shortened commit ID. */
    static final int IDLENGTH = 40;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        switch (args[0]) {
        case "init":
            initCommand(args);
            break;
        case "add":
            addCommand(args);
            break;
        case "commit":
            commitCommand(args);
            break;
        case "checkout":
            checkoutCommand(args);
            break;
        case "log":
            logCommand(args);
            break;
        case "rm":
            rmCommand(args);
            break;
        case "global-log":
            globalLogCommand(args);
            break;
        case "find":
            findCommand(args);
            break;
        case "status":
            statusCommand(args);
            break;
        case "branch":
            branchCommand(args);
            break;
        case "rm-branch":
            rmBranch(args);
            break;
        case "reset":
            resetCommand(args);
            break;
        case "merge":
            mergeCommand(args);
            break;
        default:
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }

    /** Sets up persistence of gitlet. */
    public static void setupPersistence() {
        if (!GITLET.exists()) {
            GITLET.mkdir();
        }
        if (!STAGE.exists()) {
            STAGE.mkdir();
        }
        if (!OBJECTS.exists()) {
            OBJECTS.mkdir();
        }
        if (!NODES.exists()) {
            NODES.mkdir();
        }
        if (!REMOVE.exists()) {
            REMOVE.mkdir();
        }
        if (!HEAD.exists()) {
            HEAD.mkdir();
        }
    }

    /** The amazing merge command for Gitlet. Args ARGS. */
    public static void mergeCommand(String[] args) {
        validateNumArgs("merge", args, 2);
        CommitTree initial = getTree();
        String branch = args[1];
        initial.merge(branch);
        updateHead(initial);
    }

    /** The reset command for Gitlet. Args ARGS. */
    public static void resetCommand(String[] args) {
        validateNumArgs("reset", args, 2);
        CommitTree initial = getTree();
        String commitID = args[1];
        initial.reset(commitID);
        updateHead(initial);
    }

    /** The rm-branch command for Gitlet. Args ARGS. */
    public static void rmBranch(String[] args) {
        validateNumArgs("rm-branch", args, 2);
        String name = args[1];
        CommitTree initial = getTree();
        initial.removeBranch(name);
        updateHead(initial);
    }

    /** The branch command for Gitlet. Args ARGS.*/
    public static void branchCommand(String[] args) {
        validateNumArgs("branch", args, 2);
        String branchName = args[1];
        CommitTree initial = getTree();
        initial.branch(branchName);
        updateHead(initial);
    }

    /** The status command for Gitlet. Args ARGS.*/
    public static void statusCommand(String[] args) {
        validateNumArgs("status", args, 1);
        CommitTree initial = getTree();
        initial.status();
    }

    /** The find command for Gitlet. Args ARGS.*/
    public static void findCommand(String[] args) {
        validateNumArgs("find", args, 2);
        CommitTree initial = getTree();
        initial.find(args[1]);
    }

    /** The global-log command for Gitlet. Args ARGS.*/
    public static void globalLogCommand(String[] args) {
        validateNumArgs("global-log", args, 1);
        CommitTree initial = getTree();
        initial.globalLog();
    }

    /** The rm command for Gitlet. Args ARGS.*/
    public static void rmCommand(String[] args) {
        validateNumArgs("rm", args, 2);
        String filename = args[1];
        CommitTree initial = getTree();
        initial.removeFile(filename);
        updateHead(initial);
    }

    /** The init command for Gitlet. Args ARGS.*/
    public static void initCommand(String[] args) {
        validateNumArgs("init", args, 1);
        if (NODES.exists()) {
            System.out.println("A Gitlet version-control "
                + "system already exists in the current directory.");
            System.exit(0);
        }
        setupPersistence();
        CommitTree initial = new CommitTree("initial commit");
        updateHead(initial);
    }

    /** The add command for Gitlet. Args ARGS.*/
    public static void addCommand(String[] args) {
        validateNumArgs("add", args, 2);
        String filename = args[1];
        File file = new File(filename);
        CommitTree initial = getTree();
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        String fileID = Utils.sha1((Object) Utils.readContents(file));
        File blobFile = new File(STAGE, filename);
        File stageRemoveFile = new File(REMOVE, filename);
        if (initial.getHead().getFiles() != null
                && initial.getHead().getFiles().containsKey(filename)) {
            Blob blob
                = Blob.fromFileObj(initial.getHead().getFiles().get(filename));
            if (blob.getContents().equals(Utils.readContentsAsString(file))) {
                if (blobFile.exists()) {
                    blobFile.delete();
                }
                if (stageRemoveFile.exists()) {
                    stageRemoveFile.delete();
                }
                System.exit(0);
            }
        }
        if (blobFile.exists()) {
            Blob blob = Utils.readObject(blobFile, Blob.class);
            blob.setFile(file);
        } else {
            Blob blob = new Blob(file);
            blob.saveBlob();
        }
        if (stageRemoveFile.exists()) {
            stageRemoveFile.delete();
        }
        updateHead(initial);
    }

    /** The amazing commit command for Gitlet. Args ARGS.*/
    public static void commitCommand(String[] args) {
        validateNumArgs("commit", args, 2);
        if (Objects.requireNonNull(STAGE.listFiles()).length
            == 0 && Objects.requireNonNull(REMOVE.listFiles()).length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        String msg = args[1];
        if (msg.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        CommitTree initial = getTree();
        initial.commit(msg);
        updateHead(initial);
    }

    /** The checkout command for Gitlet. Args ARGS.*/
    public static void checkoutCommand(String[] args) {
        validateNumArgs("checkout", args, 3);
        CommitTree initial = getTree();
        if (initial.getHead().getFiles() != null) {
            if (args.length == 3) {
                String filename = args[2];
                Blob blob = Utils.readObject(
                    new File(OBJECTS,
                    initial.getHead().getFiles().get(filename)), Blob.class);
                initial.checkout(blob);
            } else if (args.length == 4) {
                String commitID = args[1];
                String filename = args[3];
                if (commitID.length() < IDLENGTH) {
                    commitID = initial.findCommitID(commitID);
                }
                File file = new File(NODES, commitID);
                if (file.exists()) {
                    CommitNode node = Utils.readObject(file,
                            CommitNode.class);
                    if (node.getFiles().containsKey(filename)) {
                        Blob blob = Utils.readObject(new File(OBJECTS,
                                node.getFiles().get(filename)), Blob.class);
                        initial.checkout(commitID, blob);
                    } else {
                        System.out.println("File does not exist in "
                                + "that commit.");
                        System.exit(0);
                    }
                } else {
                    System.out.println("No commit with that id exists.");
                    System.exit(0);
                }
            } else {
                initial.checkout(args[1]);
            }
        } else {
            initial.checkout(args[1]);
        }
        updateHead(initial);
    }

    /** The log command for Gitlet. Args ARGS.*/
    public static void logCommand(String[] args) {
        validateNumArgs("log", args, 1);
        CommitTree initial = getTree();
        initial.log();
    }

    /** Updates the CommitTree stored in HEAD to initial. Tree INITIAL.*/
    private static void updateHead(CommitTree initial) {
        File file = Utils.join(HEAD, "currentHead");
        Utils.writeObject(file, initial);
    }

    /** Returns the CommitTree stored in HEAD. Args ARGS.*/
    private static CommitTree getTree() {
        return Utils.readObject(new File(HEAD,
                "currentHead"), CommitTree.class);
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (!GITLET.exists() && !cmd.equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (cmd.equals("checkout")) {
            if (args.length == 3 && !args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            } else if (args.length == 4 && !args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            } else if (args.length == 2) {
                return;
            }
            return;
        }
        if (args.length != n) {
            if (cmd.equals("commit")) {
                System.out.println("Please enter a commit message.");
                System.exit(0);
            }
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
