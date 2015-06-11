package yumyai.poseest.mmd.motion

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder

import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.border.Border
import javax.swing.border.LineBorder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class ViewAndMarkMmdMotionList extends JFrame implements ActionListener {
    static final SAVE_COMMAND = "Save"
    static final EXIT_COMMAND = "Exit"

    JButton[] buttons
    JTextArea[] fileNameTextAreas;
    boolean dirty = false
    def buttonBgColor = [
            0: Color.green,
            1: Color.red,
            2: Color.black
    ]
    int numColumns = 4
    int numRows = 4
    int batchStartIndex = 0
    int currentIndex = 0

    JLabel statusBar
    def dataFileName
    def data

    public static void main(String[] args) {
        if (args.length < 0) {
            System.out.println("Usage: java yumyai.poseest.mmd.motion.ViewAndMarkMmdMotionList <file-name>")
            System.exit(-1)
        }

        def slurper = new JsonSlurper()
        final def data = slurper.parseText(new File(args[0]).text)
        final String dataFileName = args[0]

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ViewAndMarkMmdMotionList(dataFileName, data).run()
            }
        });
    }

    public ViewAndMarkMmdMotionList(dataFileName, data) {
        super("View and Mark MMD Motion List")
        this.dataFileName = dataFileName
        this.data = data
    }

    public void run() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitProgram()
            }
        })

        createImagePanel()
        initializeMenuBar()
        createBottomPanel()
        updateState()

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createImagePanel() {
        JPanel imagePanel = new JPanel();
        GridLayout layout = new GridLayout(numColumns, numRows, 10, 10);
        imagePanel.setLayout(layout);

        int imageWidth = 0
        int imageHeight = 0
        buttons = new JButton[numColumns*numRows];
        fileNameTextAreas = new JTextArea[numColumns*numRows]
        for (int i = 0; i < 16; i++) {
            ImageIcon image = new ImageIcon(data[i].animation_name);
            def button = new JButton(image);
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            buttons[i] = button;
            fileNameTextAreas[i] = new JTextArea();
            JScrollPane scrollPane = new JScrollPane(fileNameTextAreas[i])
            panel.add(buttons[i], BorderLayout.CENTER)
            panel.add(scrollPane, BorderLayout.SOUTH)
            imagePanel.add(panel);
            scrollPane.setPreferredSize(new Dimension(212, 40))
            button.setBackground(buttonBgColor[data[i].usage_flag])
            button.addActionListener(this);
            fileNameTextAreas[i].setText(data[i].file_name.substring("data/mmd_motions/data/".length()))
        }

        def scrollPane = new JScrollPane(imagePanel)
        scrollPane.setPreferredSize(new Dimension(1300, 1300));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel();
        add(bottomPanel, BorderLayout.SOUTH);
        statusBar = new JLabel("");
        bottomPanel.add(statusBar, BorderLayout.CENTER);
        statusBar.setPreferredSize(new Dimension(numColumns * 160, 20));
    }

    private void initializeMenuBar() {
        def builder = new SwingBuilder()
        def menuBar = builder.menuBar() {
            menu(text: 'File', mnemonic: 'f') {
                menuItem(text: "Save", mnemonic: 's', accelerator: shortcut('ctrl S'), actionPerformed: {
                    save()
                    println("Saved.")
                })
                menuItem(text: "Exit", mnemonic: 'x', accelerator: shortcut('ctrl Q'), actionPerformed: {
                    exitProgram()
                })
            }
            menu(text: 'Usage', mnemonic: 'U') {
                menuItem(text: "Set flag to 'Use'", accelerator: KeyStroke.getKeyStroke('1'), actionPerformed: {
                    setCurrentFileUsage(0)
                })
                menuItem(text: "Set flag to 'Do not use'", accelerator: KeyStroke.getKeyStroke('2'), actionPerformed: {
                    setCurrentFileUsage(1)
                })
                menuItem(text: "Set flag to 'Undetermined'", accelerator: KeyStroke.getKeyStroke('3'), actionPerformed: {
                    setCurrentFileUsage(2)
                })
            }
            menu(text: 'Navigate', mnemonic: 'N') {
                menuItem(text: "UP", accelerator: KeyStroke.getKeyStroke('W'), actionPerformed: {
                    currentIndex -= numColumns
                    if (currentIndex < 0)
                        currentIndex = 0
                    updateState()
                })
                menuItem(text: "Down", accelerator: KeyStroke.getKeyStroke("S"), actionPerformed: {
                    currentIndex += numColumns
                    if (currentIndex >= data.size())
                        currentIndex = data.size() - 1
                    updateState()
                })
                menuItem(text: "Left", accelerator: KeyStroke.getKeyStroke("A"), actionPerformed: {
                    currentIndex -= 1
                    if (currentIndex < 0)
                        currentIndex = 0
                    updateState()
                })
                menuItem(text: "right", accelerator: KeyStroke.getKeyStroke("D"), actionPerformed: {
                    currentIndex += 1
                    if (currentIndex >= data.size())
                        currentIndex = data.size() - 1
                    updateState()
                })
                menuItem(text: "Page Up", accelerator: KeyStroke.getKeyStroke("ctrl A"), actionPerformed: {
                    currentIndex -= numColumns * numRows
                    if (currentIndex < 0)
                        currentIndex = 0
                    updateState()
                })
                menuItem(text: "Page Down", accelerator: KeyStroke.getKeyStroke("ctrl Z"), actionPerformed: {
                    currentIndex += numColumns * numRows
                    if (currentIndex >= data.size())
                        currentIndex = data.size() - 1
                    updateState()
                })
                menuItem(text: "Go to first 'Undetermined' file", accelerator: shortcut("ctrl D"), actionPerformed: {
                    int first = -1;
                    for (int i = 0; i < data.size(); i++) {
                        if (data[i].usage_flag == 2) {
                            first = i
                            break
                        }
                    }
                    if (first != -1) {
                        currentIndex = first
                    } else {
                        println("No files with 'Undetermined' flag.")
                        println("No files with 'Undetermined' flag.")
                    }
                    updateState()
                })
            }
        }

        setJMenuBar(menuBar);
    }

    def setCurrentFileUsage(int value) {
        dirty = value != data[currentIndex].usage_flag
        data[currentIndex].usage_flag = value
        currentIndex = Math.min((currentIndex + 1), data.size() - 1)
        updateState()
    }

    Border thickBorder = new LineBorder(Color.BLUE, 12);

    Object updateState() {
        if (currentIndex < batchStartIndex || currentIndex >= batchStartIndex + numColumns * numRows) {
            while (currentIndex < batchStartIndex) {
                batchStartIndex -= numColumns
            }
            while (currentIndex >= batchStartIndex + numColumns * numRows) {
                batchStartIndex += numColumns;
            }
            //batchStartIndex = currentIndex / (numColumns*numRows) * numColumns*numRows
            for (int i = 0; i < numColumns * numRows; i++) {
                ImageIcon image = new ImageIcon(data[batchStartIndex + i].animation_name);
                buttons[i].setIcon(image)
                fileNameTextAreas[i].setText(data[batchStartIndex+i].file_name)
            }
        }
        for (int i = 0; i < numColumns * numRows; i++) {
            buttons[i].setBackground(buttonBgColor[data[batchStartIndex + i].usage_flag])
            buttons[i].setBorder(null);
        }
        buttons[currentIndex - batchStartIndex].setBorder(thickBorder)
        updateStatusBar()
    }

    def updateStatusBar() {
        int total = data.size();
        int use = 0;
        int notUse = 0;
        int undefined = 0;
        for (int i = 0; i < data.size(); i++) {
            switch (data[i].usage_flag) {
                case 0:
                    use++;
                    break;
                case 1:
                    notUse++;
                    break;
                case 2:
                    undefined++;
                    break;
            }
        }

        int divisor = Math.max(1, use + notUse);
        String text = String.format("Use = %d, Don't use = %d, Undefined = %d, Total = %d; Pos/(Pos+Neg) = %.3f, Neg/(Pos+Neg) = %.3f",
                use, notUse, undefined, total, use * 100.0 / (divisor), notUse * 100.0 / (divisor));
        statusBar.setText(text)
    }

    def exitProgram() {
        if (dirty) {
            Object[] options = ["Save", "Exit without Saving", "Cancel"] as Object[];
            int response = JOptionPane.showOptionDialog(this,
                    "You have made some changes that you have not made. "
                            + "Are you sure you want to create new data?",
                    "New Data Confirmation",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (response == 0) {
                save()
                terminate();
            } else if (response == 1) {
                terminate();
            }
        } else {
            terminate();
        }
    }

    def terminate() {
        System.exit(0);
    }

    def save() {
        new File(dataFileName).withWriter("UTF-8") { fout ->
            fout.write(new JsonBuilder(data).toPrettyString())
        }
        dirty = false
    }

    @Override
    void actionPerformed(ActionEvent e) {
        for (int i = 0; i < numColumns * numRows; i++) {
            if (e.getSource() == buttons[i]) {
                currentIndex = batchStartIndex + i
                updateState()
            }
        }
    }
}
