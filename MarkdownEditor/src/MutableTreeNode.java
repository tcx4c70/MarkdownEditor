/**
 * Created by tcx4c70 on 16-12-12
 */
import javax.swing.tree.DefaultMutableTreeNode;

class MutableTreeNode extends DefaultMutableTreeNode {
    int startIndex, endIndex;

    public MutableTreeNode(int startIndex) {
        super();
        startIndex = 0;
        endIndex = 0;
    }

    public MutableTreeNode(Object userObject) {
        super(userObject);
        startIndex = 0;
        endIndex = 0;
    }

    public MutableTreeNode(Object userObject, boolean allowChildren) {
        super(userObject, allowChildren);
        startIndex = 0;
        endIndex = 0;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
}
