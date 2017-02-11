/**
 * Created by TCX4C70 on 16/11/30 030.
 */
import org.apache.commons.codec.binary.Hex;
import org.docx4j.Docx4J;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RFonts;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.mozilla.universalchardet.UniversalDetector;
import org.pegdown.*;
import org.pegdown.ast.HeaderNode;
import org.pegdown.ast.HtmlBlockNode;
import org.pegdown.ast.Node;
import org.pegdown.ast.RootNode;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.UndoManager;

public class MarkdownEditor extends JFrame implements ActionListener, ReceiveListener {
    private final static String programName = "MarkdownEditor";

    private JSplitPane splitPane;
    private JScrollPane textScroll, previewScroll;
    private JTextArea textView;
    private JEditorPane previewView;
    private LayoutManager layout;

    private JMenuItem openItem, newItem, saveItem, loadCssItem, exportHtmlItem, exportDocxItem, closeItem;
    private JMenuItem undoItem, redoItem, cutItem, copyItem, pasteItem, selectAllItem;
    private JMenuItem connectItem, disconnectItem;
    private JButton   openBtn, newBtn, saveBtn, closeBtn;
    private JButton   undoBtn, redoBtn, cutBtn, copyBtn, pasteBtn, selectAllBtn;
    private JCheckBoxMenuItem previewItem, structItem;

    private boolean previewVisible, structVisible;

    private JTree tree;
    private DefaultTreeModel treeModel;
    private MutableTreeNode root;

    private ImageIcon normalIcon, modifiedIcon;

    private boolean isModified;
    private boolean isNewFile;
    private File mdFile;
    private File cssFile;

    private UndoManager undoManager;

//    private Socket client;
//    ObjectOutputStream outputToServer;
    private boolean isConnecting, canSend;
    private Client client;

    public MarkdownEditor() {
        normalIcon = new ImageIcon(this.getClass().getResource("/images/normal.png"));
        modifiedIcon = new ImageIcon(this.getClass().getResource("/images/modified.png"));
        undoManager = new UndoManager();
        initUI();

        isConnecting = false;
        canSend = true;
        client = new Client();

        cssFile = new File("css/markdown-github.css");
        loadCss();

        newFile();
    }

    private void initUI() {
        previewVisible = true;
        structVisible = false;
        initMenuBar();
        initToolBar();

        textView = new JTextArea();
//        textView.setLineWrap(true);
        textView.setDragEnabled(true);
        textView.setTabSize(4);
        textView.setFont(new Font("Simsun", Font.PLAIN, 20));

        javax.swing.text.Document document = textView.getDocument();
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                preview();
                isModified = true;
                updateTitleBar();

                if(isConnecting && canSend) {
                    client.send(textView.getText());
                }

                saveItem.setEnabled(true);
                saveBtn.setEnabled(true);
                undoItem.setEnabled(undoManager.canUndo());
                undoBtn.setEnabled(undoManager.canUndo());
                redoItem.setEnabled(undoManager.canRedo());
                redoBtn.setEnabled(undoManager.canRedo());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                preview();
                isModified = true;
                updateTitleBar();

                if(isConnecting && canSend) {
                    client.send(textView.getText());
                }

                saveItem.setEnabled(true);
                saveBtn.setEnabled(true);
                undoItem.setEnabled(undoManager.canUndo());
                undoBtn.setEnabled(undoManager.canUndo());
                redoItem.setEnabled(undoManager.canRedo());
                redoBtn.setEnabled(undoManager.canRedo());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        document.addUndoableEditListener(undoManager);
        textView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);

                boolean canCutOrCopy = textView.getSelectionStart() != textView.getSelectionEnd();

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable contents = clipboard.getContents(null);
                boolean canPaste = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);

                cutItem.setEnabled(canCutOrCopy);
                cutBtn.setEnabled(canCutOrCopy);
                copyItem.setEnabled(canCutOrCopy);
                copyBtn.setEnabled(canCutOrCopy);
                pasteItem.setEnabled(canPaste);
                pasteBtn.setEnabled(canPaste);
            }
        });

        textScroll = new JScrollPane(textView);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createLoweredBevelBorder()
        ));

        previewView = new JEditorPane();
        previewView.setContentType("text/html");
        previewView.setEditable(false);
        previewView.setEditorKit(new HTMLEditorKit());

        previewScroll = new JScrollPane(previewView);
        previewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createLoweredBevelBorder()
        ));

        JScrollBar textVSB = textScroll.getVerticalScrollBar(),
                previewVSB = previewScroll.getVerticalScrollBar();

        // set the two vertical scrollbar scroll at the same time
        textVSB.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                double textVSBMin = textVSB.getMinimum(),
                        textVSBMax = textVSB.getMaximum(),
                        textVSBVisibleAmount = textVSB.getVisibleAmount();
                double previewVSBMin = previewVSB.getMinimum(),
                        previewVSBMax = previewVSB.getMaximum(),
                        previewVSBVisibleAmount = previewVSB.getVisibleAmount();
                double percent = textVSB.getValue() / (textVSBMax - textVSBMin - textVSBVisibleAmount);
                // remove the AdjustmentListener of previewScrollPane temporarily
                AdjustmentListener listener = previewVSB.getAdjustmentListeners()[0];
                previewVSB.removeAdjustmentListener(listener);
                // set the value of scrollbar in previewScroll
                previewVSB.setValue((int)(previewVSBMin + percent * (previewVSBMax - previewVSBMin - previewVSBVisibleAmount)));
                // add the AdjustmentListener of previewScroll
                previewVSB.addAdjustmentListener(listener);
            }
        });
        previewVSB.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if(previewVisible) {
                    double textVSBMin = textVSB.getMinimum(),
                            textVSBMax = textVSB.getMaximum(),
                            textVSBVisibleAmount = textVSB.getVisibleAmount();
                    double previewVSBMin = previewVSB.getMinimum(),
                            previewVSBMax = previewVSB.getMaximum(),
                            previewVSBVisibleAmount = previewVSB.getVisibleAmount();
                    double percent = previewVSB.getValue() / (previewVSBMax - previewVSBMin - previewVSBVisibleAmount);
                    // remove the AdjustmentListener of textScroll
                    AdjustmentListener listener = textVSB.getAdjustmentListeners()[0];
                    textVSB.removeAdjustmentListener(listener);
                    // set the value of scrollbar in textScroll
                    textVSB.setValue((int) (textVSBMin + percent * (textVSBMax - textVSBMin - textVSBVisibleAmount)));
                    // add the AdjustmentListener of textScroll
                    textVSB.addAdjustmentListener(listener);
                }
            }
        });

        root = new MutableTreeNode("document");

        treeModel = new DefaultTreeModel(root);

        tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                MutableTreeNode node = (MutableTreeNode)tree.getLastSelectedPathComponent();

                if(node == null)
                    return;

                int startIndex = node.getStartIndex(),
                        endIndex = node.getEndIndex();
                double textVSBMax = textVSB.getMaximum(),
                        textVSBMin = textVSB.getMinimum();
                try {
                    double percent = (double)textView.getLineOfOffset(startIndex) / textView.getLineCount();
                    textVSB.setValue((int) (textVSBMin + percent * (textVSBMax - textVSBMin)));
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            }
        });

        splitPane= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        add(splitPane, BorderLayout.CENTER);
        updateSplitPane();

        setSize(1120, 630);
        setVisible(true);
        splitPane.setDividerLocation(0.17);
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        // New item
        newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        newItem.setActionCommand("new");
        newItem.addActionListener(this);
        fileMenu.add(newItem);

        // Open item
        openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openItem.setActionCommand("open");
        openItem.addActionListener(this);
        fileMenu.add(openItem);

        // Save item
        saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveItem.setActionCommand("save");
        saveItem.addActionListener(this);
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        // Load css file item
        loadCssItem = new JMenuItem("Load css file");
        loadCssItem.setActionCommand("load_css");
        loadCssItem.addActionListener(this);
        fileMenu.add(loadCssItem);

        fileMenu.addSeparator();

        // Export Menu
        JMenu exportMenu = new JMenu("Export");
        fileMenu.add(exportMenu);

        // Export html item
        exportHtmlItem = new JMenuItem("Export html");
        exportHtmlItem.setActionCommand("export_html");
        exportHtmlItem.addActionListener(this);
        exportMenu.add(exportHtmlItem);

        // Export docx item
        exportDocxItem = new JMenuItem("Export docx");
        exportDocxItem.setActionCommand("export_docx");
        exportDocxItem.addActionListener(this);
        exportMenu.add(exportDocxItem);

        // Export pdf item
        //JMenuItem exportPdfItem = new JMenuItem("Export pdf");
        //exportMenu.add(exportPdfItem);
        //exportPdfItem.addActionListener(new ActionListener() {
        //    @Override
        //    public void actionPerformed(ActionEvent e) {
        //        exportPdf();
        //    }
        //});

        fileMenu.addSeparator();

        // Close item
        closeItem = new JMenuItem("Close");
        closeItem.setActionCommand("close");
        closeItem.addActionListener(this);
        fileMenu.add(closeItem);

        // Edit menu
        JMenu edit = new JMenu("Edit");
        menuBar.add(edit);

        undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        undoItem.setActionCommand("undo");
        undoItem.addActionListener(this);
        edit.add(undoItem);

        redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        redoItem.setActionCommand("redo");
        redoItem.addActionListener(this);
        edit.add(redoItem);

        edit.addSeparator();

        cutItem = new JMenuItem("Cut");
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        cutItem.setActionCommand("cut");
        cutItem.addActionListener(this);
        edit.add(cutItem);

        copyItem = new JMenuItem("Copy");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        copyItem.setActionCommand("copy");
        copyItem.addActionListener(this);
        edit.add(copyItem);

        pasteItem = new JMenuItem("Paste");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        pasteItem.setActionCommand("paste");
        pasteItem.addActionListener(this);
        edit.add(pasteItem);

        edit.addSeparator();

        selectAllItem = new JMenuItem("Select All");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        selectAllItem.setActionCommand("select_all");
        selectAllItem.addActionListener(this);
        edit.add(selectAllItem);

        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        menuBar.add(toolsMenu);

        previewItem = new JCheckBoxMenuItem("Preview");
        previewItem.setActionCommand("preview");
        previewItem.setState(previewVisible);
        previewItem.addActionListener(this);
        toolsMenu.add(previewItem);

        structItem = new JCheckBoxMenuItem("Document Structure");
        structItem.setActionCommand("struct");
        structItem.setState(structVisible);
        structItem.addActionListener(this);
        toolsMenu.add(structItem);

        // CSCW Menu
        JMenu cscwMenu = new JMenu("CSCW");
        menuBar.add(cscwMenu);

        connectItem = new JMenuItem("Connect");
        connectItem.setActionCommand("connect");
        connectItem.addActionListener(this);
        cscwMenu.add(connectItem);

        disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.setActionCommand("disconnect");
        disconnectItem.addActionListener(this);
        disconnectItem.setEnabled(false);
        cscwMenu.add(disconnectItem);
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar("Tool Bar");
        add(toolBar, BorderLayout.PAGE_START);

        newBtn = addButtonToToolBar(toolBar, "new");
        openBtn = addButtonToToolBar(toolBar, "open");
        saveBtn = addButtonToToolBar(toolBar, "save");
        closeBtn = addButtonToToolBar(toolBar, "close");

        toolBar.addSeparator();

        undoBtn = addButtonToToolBar(toolBar, "undo");
        redoBtn = addButtonToToolBar(toolBar, "redo");

        toolBar.addSeparator();

        cutBtn = addButtonToToolBar(toolBar, "cut");
        copyBtn = addButtonToToolBar(toolBar, "copy");
        pasteBtn = addButtonToToolBar(toolBar, "paste");

        toolBar.addSeparator();

        selectAllBtn = addButtonToToolBar(toolBar, "select_all");

        toolBar.addSeparator();
    }

    private JButton addButtonToToolBar(JToolBar toolBar, String actionCommand) {
        Icon icon = new ImageIcon(this.getClass().getResource("/images/" + actionCommand + ".png"));
        JButton btn = new JButton(icon);

        btn.setActionCommand(actionCommand);
        btn.setBorderPainted(false);
        btn.addActionListener(this);
        toolBar.add(btn);

        return btn;
    }

    private void updateTitleBar() {
        if(mdFile == null) {
            setTitle(programName);
            setIconImage(normalIcon.getImage());
        }
        else if(isModified) {
            setTitle(mdFile.getName() + "* - " + programName);
            setIconImage(modifiedIcon.getImage());
        }
        else {
            setTitle(mdFile.getName() + " - " + programName);
            setIconImage(normalIcon.getImage());
        }
    }

    private void updateSplitPane() {
        if(structVisible) {
            splitPane.setLeftComponent(new JScrollPane(tree));
            splitPane.setDividerSize(5);
            splitPane.setDividerLocation(0.17);
        }
        else {
            splitPane.setLeftComponent(null);
            splitPane.setDividerSize(0);
        }

        if(previewVisible) {
            JPanel panel = new JPanel(new GridLayout(1, 2));
            panel.add(textScroll);
            panel.add(previewScroll);
            splitPane.setRightComponent(panel);
        }
        else {
            splitPane.setRightComponent(textScroll);
        }

        preview();
        textView.requestFocus();
        textView.setCaretPosition(0);
    }

    private void preview() {
        try {
            PegDownProcessor processor = new PegDownProcessor();
            RootNode rootNode = processor.parseMarkdown(textView.getText().toCharArray());
            ToHtmlSerializer serializer = new ToHtmlSerializer(new LinkRenderer());
            String html = serializer.toHtml(rootNode);
            if(previewVisible) {
                previewView.setText(html);
            }
            else {
                previewView.setText("");
            }

            // remove all nodes
            while (root.getChildCount() > 0) {
                treeModel.removeNodeFromParent((DefaultMutableTreeNode) root.getChildAt(0));
            }

            if(structVisible) {
                // parse the html
                Document doc = Jsoup.parseBodyFragment(html);
                // get all <h?> tags
                Elements hTags = doc.select("h1, h2, h3, h4, h5, h6");
                List<Node> nodes = rootNode.getChildren().stream()
                        .filter(node -> node instanceof HeaderNode || ((node instanceof HtmlBlockNode) &&
                                ((HtmlBlockNode)node).getText().matches("<h([1-6])[^>]*>.*</h\\1>")))
                        .collect(Collectors.toList());

                // add all <h?> tags to tree
                ArrayList<Map.Entry<MutableTreeNode, String>> nodeList = new ArrayList<>();
                nodeList.add(new AbstractMap.SimpleEntry<>(root, "h0"));
                for (int i = 0; i < hTags.size() && i < nodes.size(); i ++) {
                    Element element = hTags.get(i);
                    Node node = nodes.get(i);
                    while (nodeList.get(nodeList.size() - 1).getValue().compareTo(element.tagName()) >= 0) {
                        nodeList.remove(nodeList.size() - 1);
                    }
                    MutableTreeNode parent = nodeList.get(nodeList.size() - 1).getKey();
                    MutableTreeNode thisChild = new MutableTreeNode(element.text());
                    thisChild.setStartIndex(node.getStartIndex());
                    thisChild.setEndIndex(node.getEndIndex());
                    nodeList.add(new AbstractMap.SimpleEntry<>(thisChild, element.tagName()));
                    treeModel.insertNodeInto(thisChild, parent, parent.getChildCount());
                }
                tree.expandPath(new TreePath(root));
            }
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    /**
     * call when click on "New"
     */
    private void newFile() {
        int ret = showSaveConfirmDialog();
        if(ret == JOptionPane.CANCEL_OPTION ||
                ret == JOptionPane.CLOSED_OPTION)
            return;
        closeFile();
        isModified = false;
        isNewFile = true;
        mdFile = new File("Untitled.md");
        updateTitleBar();
        textView.setText("");
        textView.setCaretPosition(0);
        //preview();

        undoManager.discardAllEdits();
        textScroll.setVisible(true);
        previewScroll.setVisible(true);
        saveItem.setEnabled(false);
        saveBtn.setEnabled(false);
        exportDocxItem.setEnabled(true);
        exportHtmlItem.setEnabled(true);
        closeItem.setEnabled(true);
        closeBtn.setEnabled(true);
        undoItem.setEnabled(false);
        undoBtn.setEnabled(false);
        redoItem.setEnabled(false);
        redoBtn.setEnabled(false);
        cutItem.setEnabled(false);
        cutBtn.setEnabled(false);
        copyItem.setEnabled(false);
        copyBtn.setEnabled(false);
        pasteItem.setEnabled(false);
        pasteBtn.setEnabled(false);
        selectAllItem.setEnabled(true);
        selectAllBtn.setEnabled(true);
    }

    /**
     * call when click on "Open"
     */
    private void openFile() {
        int saveRet = showSaveConfirmDialog();
        if(saveRet == JOptionPane.CANCEL_OPTION ||
                saveRet == JOptionPane.CLOSED_OPTION)
            return;
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFileChooser.setSelectedFile(mdFile);
        openFileChooser.removeChoosableFileFilter(openFileChooser.getFileFilter());
        openFileChooser.setFileFilter(new FileNameExtensionFilter("Markdown Files (*.md, *.markdown, *.mdown)",
                "md", "markdown", "mdown"));
        int openRet = openFileChooser.showOpenDialog(this);
        if(openRet != JFileChooser.APPROVE_OPTION)
            return;
        mdFile = openFileChooser.getSelectedFile();
        try
        {
            DataInputStream input = new DataInputStream(
                new BufferedInputStream(
                new FileInputStream(mdFile)));
            byte[] mdFileContent = new byte[(int)mdFile.length()];
            input.read(mdFileContent);
            // check the charset of the file
            UniversalDetector detector = new UniversalDetector(null);
            detector.handleData(mdFileContent, 0, mdFileContent.length);
            detector.dataEnd();
            String encoding = detector.getDetectedCharset();
            // set the text of textView
            if(encoding != null)
                textView.setText(new String(mdFileContent, encoding));
            else
                textView.setText(new String(mdFileContent));
            //preview();
            input.close();
        }
        catch (IOException exp)
        {
            JOptionPane.showMessageDialog(this, "Open markdown file failed!");
            return;
        }
        isModified = false;
        isNewFile = false;
        // set the position of caret to the beginning of the file
        textView.setCaretPosition(0);
        updateTitleBar();

        undoManager.discardAllEdits();
        textScroll.setVisible(true);
        previewScroll.setVisible(true);
        saveItem.setEnabled(false);
        saveBtn.setEnabled(false);
        exportDocxItem.setEnabled(true);
        exportHtmlItem.setEnabled(true);
        closeItem.setEnabled(true);
        closeBtn.setEnabled(true);
        undoItem.setEnabled(false);
        undoBtn.setEnabled(false);
        redoItem.setEnabled(false);
        redoBtn.setEnabled(false);
        cutItem.setEnabled(false);
        cutBtn.setEnabled(false);
        copyItem.setEnabled(false);
        copyBtn.setEnabled(false);
        pasteItem.setEnabled(false);
        pasteBtn.setEnabled(false);
        selectAllItem.setEnabled(true);
        selectAllBtn.setEnabled(true);
    }

    private int showSaveConfirmDialog() {
        if (isModified) {
            int ret = JOptionPane.showConfirmDialog(this,
                    "The file has been modified. Do you want to save the current file?",
                    "MarkdownEditor", JOptionPane.YES_NO_CANCEL_OPTION);
            if(JOptionPane.YES_OPTION == ret) {
                if (isNewFile) {
                    if(JFileChooser.APPROVE_OPTION != saveNewFile())
                        ret = JOptionPane.CANCEL_OPTION;
                }
                else
                    saveFile();
            }
            return ret;
        }
        return JOptionPane.YES_OPTION;
    }

    /**
     * call when click on "Close"
     */
    private void closeFile() {
        mdFile = null;
        updateTitleBar();
        textView.setText("");
        textScroll.setVisible(false);
        previewScroll.setVisible(false);
        isModified = false;
        saveItem.setEnabled(false);
        saveBtn.setEnabled(false);
        exportDocxItem.setEnabled(false);
        exportHtmlItem.setEnabled(false);
        closeItem.setEnabled(false);
        closeBtn.setEnabled(false);
        undoItem.setEnabled(false);
        undoBtn.setEnabled(false);
        redoItem.setEnabled(false);
        redoBtn.setEnabled(false);
        selectAllItem.setEnabled(false);
        selectAllBtn.setEnabled(false);
    }

    private void saveFile() {
        try
        {
            DataOutputStream output = new DataOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(mdFile)));
            output.write(textView.getText().getBytes());
            output.close();
        }
        catch (IOException exp)
        {
            JOptionPane.showMessageDialog(this, "Save file failed!");
            return;
        }
        isModified = false;
        isNewFile = false;
        updateTitleBar();
        saveItem.setEnabled(false);
        saveBtn.setEnabled(false);
    }

    private int saveNewFile() {
        JFileChooser saveFileChooser = new JFileChooser();
        saveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        saveFileChooser.setSelectedFile(new File(mdFile.getName().substring(0, mdFile.getName().lastIndexOf('.')) + ".md"));
        saveFileChooser.removeChoosableFileFilter(saveFileChooser.getFileFilter());
        saveFileChooser.setFileFilter(new FileNameExtensionFilter("Markdown Files (*.md, *.markdown, *.mdown)",
                "md", "markdown", "mdown"));
        int ret = saveFileChooser.showSaveDialog(this);
        if(JFileChooser.APPROVE_OPTION != ret)
            return ret;
        mdFile = saveFileChooser.getSelectedFile();
        if(!saveFileChooser.getSelectedFile().getName().toLowerCase().matches(".*.(md|markdown|mdown)"))
            mdFile = new File(mdFile.getPath() + ".md");
        saveFile();
        return ret;
    }

    private int showLoadCssDialog() {
        JFileChooser loadCssFileChooser = new JFileChooser();
        loadCssFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        loadCssFileChooser.setSelectedFile(cssFile);
        loadCssFileChooser.removeChoosableFileFilter(loadCssFileChooser.getFileFilter());
        loadCssFileChooser.setFileFilter(new FileNameExtensionFilter("CSS Files (*.css)", "css"));
        int ret = loadCssFileChooser.showOpenDialog(this);
        if(ret == JFileChooser.APPROVE_OPTION)
            cssFile = loadCssFileChooser.getSelectedFile();
        return ret;
    }

    /**
     * call when click on "Load css file"
     * load css file to set html style
     */
    private void loadCss() {
        try {
            StyleSheet style = ((HTMLEditorKit)previewView.getEditorKit()).getStyleSheet();
            URL css = cssFile.toURI().toURL();
            Enumeration<?> enu = style.getStyleNames();
            ArrayList<String> styleNames = new ArrayList<>();
            while(enu.hasMoreElements()) {
                styleNames.add(enu.nextElement().toString());
            }
            for(String styleName: styleNames) {
                style.removeStyle(styleName);
            }
            style.importStyleSheet(css);
            preview();
            previewView.setCaretPosition(0);
        } catch (IOException exp) {
            JOptionPane.showMessageDialog(this, "Open css file failed!");
        }

    }

    /**
     * call when click on "Export docx"
     * export the markdown file to a word document
     */
    private void exportDocx() {
        JFileChooser exportFileChooser = new JFileChooser();
        exportFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        exportFileChooser.setSelectedFile(new File(mdFile.getPath().substring(0, mdFile.getPath().lastIndexOf('.')) + ".docx"));
        exportFileChooser.removeChoosableFileFilter(exportFileChooser.getFileFilter());
        exportFileChooser.setFileFilter(new FileNameExtensionFilter("Word Documents (*.docx)", "docx"));
        if(exportFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File generate = exportFileChooser.getSelectedFile();
        if(!generate.getName().toLowerCase().matches(".*.docx"))
            generate = new File(generate.getPath() + ".docx");
        try {
            html2Word(toHtml()).save(generate);
            JOptionPane.showMessageDialog(this, "Export docx successful!");
        } catch (Exception exp) {
            JOptionPane.showMessageDialog(this, "Export docx failed! Please try again!");
        }
    }

    /**
     * call when click on "Export html"
     * export the markdown file to a html file
     */
    private void exportHtml() {
        JFileChooser exportFileChooser = new JFileChooser();
        exportFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        exportFileChooser.setSelectedFile(new File(mdFile.getPath().substring(0, mdFile.getPath().lastIndexOf('.')) + ".html"));
        exportFileChooser.removeChoosableFileFilter(exportFileChooser.getFileFilter());
        exportFileChooser.setFileFilter(new FileNameExtensionFilter("HTML Files (*.html)", "html"));
        if(exportFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File generate = exportFileChooser.getSelectedFile();
        if(!generate.getName().toLowerCase().matches(".*.html"))
            generate = new File(generate.getPath() + ".html");
        try {
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(generate)
                    )
            );
            out.write(toHtml().getBytes());
            out.close();
            JOptionPane.showMessageDialog(this, "Export html successful!");
        } catch (IOException exp) {
            JOptionPane.showMessageDialog(this, "Export html failed! Please try again");
        }
    }

    /**
     * call when click on "Export pdf"
     */
    private void exportPdf() {
        JFileChooser exportFileChooser = new JFileChooser();
        exportFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        exportFileChooser.setSelectedFile(new File(mdFile.getPath().substring(0, mdFile.getPath().lastIndexOf('.')) + ".pdf"));
        exportFileChooser.removeChoosableFileFilter(exportFileChooser.getFileFilter());
        exportFileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        if(exportFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File generate = exportFileChooser.getSelectedFile();
        if(!generate.getName().toLowerCase().matches(".*.pdf"))
            generate = new File(generate.getPath() + ".pdf");

        try {
            OutputStream out = new FileOutputStream(generate);
            Docx4J.toPDF(html2Word(toHtml()), out);
            out.flush();
            out.close();
            JOptionPane.showMessageDialog(this, "Export pdf successful!");
        } catch (Exception exp) {
            JOptionPane.showMessageDialog(this, "Export pdf failed! Please try again!");
        }
    }

    private String toHtml() {
        String html = "", head = "", title = "", meta = "", style = "", body = "";

        title = "<title>" + mdFile.getName().substring(0, mdFile.getName().lastIndexOf('.')) + "</title>";
        meta = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />";
        try {
            if(cssFile != null && cssFile.exists()) {
                DataInputStream input = new DataInputStream(
                    new BufferedInputStream(
                        new FileInputStream(cssFile)
                    )
                );
                byte[] fileContent = new byte[(int)cssFile.length()];
                input.read(fileContent);
                UniversalDetector detector = new UniversalDetector(null);
                detector.handleData(fileContent, 0, fileContent.length);
                detector.dataEnd();
                String encoding = detector.getDetectedCharset();
                if(encoding != null)
                    style = "<style type=\"text/css\">\n" + new String(fileContent, encoding) + "\n</style>";
                else
                    style = "<style type=\"text/css\">\n" + new String(fileContent) + "\n</style>";
            }
        } catch (IOException exp) {
            exp.printStackTrace();
        }

        head = "<head>\n" + title + "\n" + meta + "\n" + style + "\n</head>";

        RootNode rootNode = new PegDownProcessor().parseMarkdown(textView.getText().toCharArray());
        body = new ToHtmlSerializer(new LinkRenderer()).toHtml(rootNode);
        body = "<body>\n" + body + "\n</body>";

        html = "<!DOCTYPE HTML>\n" + "<html>\n" + head + "\n" + body + "\n</html>";

        return html;
    }

    private WordprocessingMLPackage html2Word(String html) throws Exception {
        Document doc = Jsoup.parse(html);

        // remove all scripts
        for(org.jsoup.nodes.Element script: doc.getElementsByTag("script")) {
            script.remove();
        }

        // remove onClick and href in <a>
        for(org.jsoup.nodes.Element a: doc.getElementsByTag("a")) {
            a.removeAttr("onClick");
            a.removeAttr("href");
        }

        // replace the addresses in <link> with absolute addresses
        Elements links = doc.getElementsByTag("link");
        for(org.jsoup.nodes.Element element: links) {
            String href = element.absUrl("href");
            element.attr("href", href);
        }

        // change to xhtml
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(Entities.EscapeMode.xhtml);

        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage(PageSizePaper.A4, true);

        RFonts rFonts = Context.getWmlObjectFactory().createRFonts();
        rFonts.setAsciiTheme(null);
        rFonts.setAscii("SimSun");
        XHTMLImporterImpl.addFontMapping("SimSun", rFonts);

        XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordMLPackage);
        wordMLPackage.getMainDocumentPart().getContent().addAll(xhtmlImporter.convert(doc.html(), doc.baseUri()));

        return wordMLPackage;
    }

    private boolean connect() {
        String address;
        int port;
        while(true) {
            String server = (String)JOptionPane.showInputDialog(this, "Server(Address:Port):", "Connect to",
                    JOptionPane.PLAIN_MESSAGE, null, null, "118.89.145.205:8000");
            if (server == null)
                return false;
            int colonPos = server.lastIndexOf(':');
            if(colonPos == -1) {
                JOptionPane.showMessageDialog(this, "The format of server is 'Address:Port'!\nPlease try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            address = server.substring(0, colonPos).trim();
            String portStr = server.substring(colonPos + 1).trim();
            if(!portStr.matches("\\d+")) {
                JOptionPane.showMessageDialog(this, "The port must be number!\nPlease try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            port = Integer.parseInt(portStr);
            break;
        }

        try {
            client.connect(address, port, getHash());
            client.getReceiver().addReceiveListener(this);

            JOptionPane.showMessageDialog(this, "Connect to " + address + " successfully!");
            return true;
        } catch (IOException exp) {
            JOptionPane.showMessageDialog(this, "Fail to connect to " + address + "!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            exp.printStackTrace();
            return false;
        }
    }

    private boolean disconnect() {
        if(isConnecting) {
            try {
                client.disconnect();
                return true;
            } catch (IOException exp) {
                exp.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }

    private BigInteger getHash() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(textView.getText().getBytes());
            byte[] digest = messageDigest.digest();
            BigInteger md5 = new BigInteger(Hex.encodeHexString(digest), 16);
            System.out.println("MD5: " + md5.toString(16));
            return md5;
        } catch (NoSuchAlgorithmException exp) {
            exp.printStackTrace();
            return null;
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent event) {
        if(event.getID() == WindowEvent.WINDOW_CLOSING) {
            int ret = showSaveConfirmDialog();
            if(ret == JOptionPane.YES_OPTION || ret == JOptionPane.NO_OPTION) {
                disconnect();
                System.exit(0);
            }
        }
        else {
            super.processWindowEvent(event);
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if("new".equals(command)) {
            newFile();
        }
        else if("open".equals(command)) {
            openFile();
        }
        else if("save".equals(command)) {
            if(isNewFile)
                saveNewFile();
            else
                saveFile();
        }
        else if("load_css".equals(command)) {
            if(JFileChooser.APPROVE_OPTION == showLoadCssDialog()) {
                loadCss();
            }
        }
        else if("export_html".equals(command)) {
            exportHtml();
        }
        else if("export_docx".equals(command)) {
            exportDocx();
        }
        else if("close".equals(command)) {
            int ret = showSaveConfirmDialog();
            if(JOptionPane.YES_OPTION == ret || JOptionPane.NO_OPTION == ret)
                closeFile();
        }
        else if("undo".equals(command)) {
            undoManager.undo();
        }
        else if("redo".equals(command)) {
            undoManager.redo();
        }
        else if("cut".equals(command)) {
            textView.cut();
        }
        else if("copy".equals(command)) {
            textView.copy();
        }
        else if("paste".equals(command)) {
            textView.paste();
        }
        else if("select_all".equals(command)) {
            textView.requestFocus();
            textView.selectAll();
            cutItem.setEnabled(true);
            cutBtn.setEnabled(true);
            copyItem.setEnabled(true);
            copyBtn.setEnabled(true);
        }
        else if("preview".equals(command)) {
            previewVisible = previewItem.getState();
            updateSplitPane();
        }
        else if("struct".equals(command)) {
            structVisible = structItem.getState();
            updateSplitPane();
        }
        else if("connect".equals(command)) {
            if(connect()) {
                isConnecting = true;
                connectItem.setEnabled(false);
                disconnectItem.setEnabled(true);
            }
        }
        else if("disconnect".equals(command)) {
            if(disconnect()) {
                isConnecting = false;
                disconnectItem.setEnabled(false);
                connectItem.setEnabled(true);
            }
        }
    }

    @Override
    public void onReceive(ReceiveEvent event) {
        if(Action.ActionType.Edit.equals(event.getAction().getActionType())) {
            canSend = false;
            textView.setText(event.getAction().getEditText());
            canSend = true;
        }
    }
}
