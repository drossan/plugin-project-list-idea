package com.drossan.projectlist

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.swing.*

class ListDirAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        if (project != null) {
            val dialog = DirectoryListDialog(project)
            if (dialog.showAndGet()) {
                val dir = if (dialog.directoryField.text.trim().isEmpty()) project.basePath ?: "" else dialog.directoryField.text.trim()
                val ignoredDirs = if (dialog.ignoredDirsField.text.trim().isEmpty()) {
                    setOf(".git", "node_modules", "vendor", ".idea", ".vsc")
                } else {
                    dialog.ignoredDirsField.text.split(",").map { it.trim() }.toSet()
                }
                val createFile = dialog.createFileCheckBox.isSelected
                val result = StringBuilder()
                result.append("/${File(dir).name}/\n")

                try {
                    listFiles(dir, "", ignoredDirs, result)
                    if (createFile) {
                        val outputFile = File(project.basePath, "list_dir_output.txt")
                        BufferedWriter(FileWriter(outputFile)).use { writer ->
                            writer.write(result.toString())
                        }
                        Messages.showInfoMessage("Directory listing completed and saved to ${outputFile.path}", "Success")
                    }
                    // Show result in a window
                    showResultWindow(project, result.toString())
                } catch (e: IOException) {
                    Messages.showErrorDialog(project, "Error listing files: ${e.message}", "Error")
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun listFiles(dir: String?, prefix: String, ignoredDirs: Set<String>, result: StringBuilder) {
        val directory = File(dir)
        val entries = directory.listFiles() ?: return

        for (entry in entries) {
            if (ignoredDirs.contains(entry.name)) continue

            val output = "$prefix|-- ${entry.name}\n"
            result.append(output)

            if (entry.isDirectory) {
                listFiles(entry.path, "$prefix|   ", ignoredDirs, result)
            }
        }
    }

    private fun showResultWindow(project: Project, result: String) {
        val dialog = object : DialogWrapper(project) {
            init {
                init()
                title = "Directory Listing Result"
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout())
                val textArea = JTextArea(result)
                textArea.isEditable = false
                panel.add(JScrollPane(textArea), BorderLayout.CENTER)
                return panel
            }
        }
        dialog.show()
    }

    class DirectoryListDialog(project: Project) : DialogWrapper(true) {
        val directoryField = JTextField("", 30)
        val ignoredDirsField = JTextField("", 30)
        val createFileCheckBox = JCheckBox("Create file list_dir_output.txt", true)

        init {
            init()
            title = "Directory Listing Options"
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout())
            val inputPanel = JPanel()
            inputPanel.layout = BoxLayout(inputPanel, BoxLayout.Y_AXIS)

            inputPanel.add(JLabel("Enter the directory to list (default is project root):"))
            inputPanel.add(directoryField)
            inputPanel.add(Box.createVerticalStrut(10))
            inputPanel.add(JLabel("Enter directories to ignore, separated by commas (default is .git,node_modules,vendor,.idea,.vsc):"))
            inputPanel.add(ignoredDirsField)
            inputPanel.add(Box.createVerticalStrut(10))
            inputPanel.add(createFileCheckBox)

            panel.add(inputPanel, BorderLayout.CENTER)
            return panel
        }
    }
}
