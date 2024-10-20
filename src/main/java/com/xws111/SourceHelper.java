package com.xws111;


import java.io.File;
import java.util.Map;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;



/**
 * @Description: 将 ".class" 文件的源代码，导出为 md 格式的文档，方便自己进行注释和阅读。
 * @date: 2024/10/18 8:51
 * @author: xws111
 **/
public class SourceHelper extends AnAction {

    /**
     * 点击后触发的动作
     * @param event AnActionEvent
     */
    @Override
    public void actionPerformed(AnActionEvent event) {
        // 获取当前项目
        Project project = event.getProject();
        if (project == null) {
            Messages.showMessageDialog("No active project found.", "Error", Messages.getErrorIcon());
            return;
        }

        // 获取当前文件
        VirtualFile virtualFile = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            Messages.showMessageDialog("No file found.", "Error", Messages.getErrorIcon());
            return;
        }


        Exporter exporter = new Exporter(project);
        exporter.exportToMarkdown(virtualFile);


    }


}
