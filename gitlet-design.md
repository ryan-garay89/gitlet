# Gitlet Design Doc

# Classes and Data Structures 
## CommitTree 

This class represents a tree of CommitNode’s, storing a pointer to the head / current commit.

**Fields**

1. `CommitNode _head`: A pointer to the most recent commit in the tree.
2. `HashMap<String, CommitNode> _branches`: A mapping of the branches in the tree
## CommitNode

Represents a commit, storing metadata about the commit.

**Fields**

1. `String _log`: the log message from the user about this commit
2. `String _date`: the date of this commit
3. `String _time`: the time at which this commit was made
4. `String ID`: the hash ID of this commit
5. `HashMap<String, Blob> _files`: A mapping of file names to blob references 
6. `CommitNode _parent`: parent of this commit node.
## Blob implements Serializable 

Contents of files 

**Fields**

1. `File _file`: the current file represented by the blob
2. `String ID`: the has ID of this file
3. `File STAGE`: the directory of the stage folder


## Main

Executes command-line arguments input by the user


# Algorithms 
## CommitTree Class
1. `CommitTree()`: The constructor of the CommitTree class; initializes the `_head` pointer to null.
2. `add()`: adds a new commit to the tree by creating a new node with its parent as the original version of the commit.
3. `remove()`: Removes the commit pointed to by `_head`, and updates the pointer.
4. `find(String msg)`: Follows the tree from the head pointer backwards, printing out ID’s of all commits with a message == msg
5. `branch()`: Creates a new pointer to the commitNode pointed to by `_head`, and adds it to `_branches`
6. `head()`: Returns the head of the CommitTree.
7. `removeBranch(String name)`: removes the branch with the given name.
8. `checkout(String name)`: sets `_head` to the branch in `_branches` with given name.
9. `deleteBranch(String name)`: deletes the branch in `_branches` with given name.
10. `getBranches()`: Returns `_branches`.
11. `Merge(CommitNode branch, CommitNode master)`: Finds the latest common ancestor of two nodes and merges them.


## CommitNode class
1. `CommitNode(String msg, List<Blob> files, CommitNode parent)`: Constructor of a CommitNode; setting its log to msg, _files to files, and parent to _parent (or null). Gets metadata by calling `timestamp()`.
2. `getFile(String name)`: Returns the file in `_files` with given name
3. `snapShot()`: Saves a snapshot of certain files to the staging area. First, serializes the correct files, then adds them to the staging area folder.
4. `removeFile(String name)`: removes the file with the given name from this commit.
5. `timestamp()`: assigns `_date` and `_time` to when this commit was created.
6. `getMetadata()`: Returns the date and time this commit was created


## Blob Class
1. `Blob(File file)`: Initializes `_file` to file and gets its SHA-1 ID.
2. `fromFile()`: Reads in a deserializes this blob.


## Main Class
1. `clear()`: clears the staging area after a commit
2. `command(String cmd)`: executes the correct command based on the string cmd
3. `remove()`: executes the remove command
4. `init()`: executes the init command
5. `add()`: executes the add command 
6. `commit()`: executes the commit command 
7. `main()`
8. `log()` executes the log command.
9. `find()`
10. `status()`
11. `checkout()`
12. `branch()`
13. `reset()`
14. `merge()`
15. `rm-branch()`


# Persistence 

Throughout Gitlet, it is required to retain information about saved files, commits, and commit history. For example, the log command requires metadata from certain commitNodes, which will require that these nodes are saved for future use. This means that:


1. Each Blob has the ability to become serialized and de-serialized, as they are accessed by the CommitNode’s of which they are associated with.
2. The CommitNode’s and CommitTree have the ability to become serialized, and their pointers maintained through SHA-1 IDs associated with each object. 


