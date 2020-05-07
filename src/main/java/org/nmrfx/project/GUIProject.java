/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.project;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.concurrent.Task;

import javafx.stage.Stage;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.nmrfx.processor.gui.FXMLController;
import org.nmrfx.processor.gui.GUIScripter;
import org.nmrfx.processor.gui.MainApp;
import org.nmrfx.processor.gui.PreferencesController;
import org.python.util.PythonInterpreter;

/**
 *
 * @author Bruce Johnson
 */
public class GUIProject extends Project {

    Git git;

    private static boolean commitActive = false;

    public GUIProject(String name) {
        super(name);
    }

    public static GUIProject replace(String name, GUIProject project) {
        GUIProject newProject = new GUIProject(name);
        newProject.datasetMap = project.datasetMap;
        newProject.peakListTable = project.peakListTable;
        newProject.resFactory = project.resFactory;
        newProject.peakPaths = project.peakPaths;
        return newProject;
    }

    public static GUIProject getActive() {
        Project project = Project.getActive();
        if (project == null) {
            project = new GUIProject("Untitled 1");
        }
        return (GUIProject) project;
    }

    public void createProject(Path projectDir) throws IOException {
        try {
            super.createProject(projectDir);
            PreferencesController.saveRecentProjects(projectDir.toString());
            git = Git.init().setDirectory(projectDir.toFile()).call();
        } catch (GitAPIException ex) {
            Logger.getLogger(GUIProject.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (git != null) {
            writeIgnore();
        }
    }

    private void writeIgnore() {
        if (git != null) {
            Path path = Paths.get(projectDir.toString(), ".gitignore");
            try (FileWriter writer = new FileWriter(path.toFile())) {
                writer.write("*.nv\n*.ucsf");
            } catch (IOException ioE) {
                System.out.println(ioE.getMessage());
            }
        }
    }

    public void loadGUIProject(Path projectDir) throws IOException, IllegalStateException {
        Project currentProject = getActive();
        setActive();

        loadProject(projectDir);

        if (currentProject == this) {
            FileSystem fileSystem = FileSystems.getDefault();

            String[] subDirTypes = {"windows"};
            if (projectDir != null) {
                for (String subDir : subDirTypes) {
                    Path subDirectory = fileSystem.getPath(projectDir.toString(), subDir);
                    if (Files.exists(subDirectory) && Files.isDirectory(subDirectory) && Files.isReadable(subDirectory)) {
                        switch (subDir) {
                            case "windows":
                                loadWindows(subDirectory);
                                break;
                            default:
                                throw new IllegalStateException("Invalid subdir type");
                        }
                    }

                }
            }
        }
        this.projectDir = projectDir;
        PreferencesController.saveRecentProjects(projectDir.toString());
        currentProject.setActive();
    }

    @Override
    public void saveProject() throws IOException {
        Project currentProject = getActive();
        setActive();

        if (projectDir == null) {
            throw new IllegalArgumentException("Project directory not set");
        }
        super.saveProject();
        if (currentProject == this) {
            saveWindows();
        }
        gitCommitOnThread();
        PreferencesController.saveRecentProjects(projectDir.toString());
        currentProject.setActive();
    }

    void gitCommitOnThread() {
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return gitCommit();
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public static boolean isCommitting() {
        return commitActive;
    }

    boolean gitCommit() {
        boolean didSomething = false;
        commitActive = true;
        try {
            if (git == null) {
                try {
                    git = Git.open(projectDir.toFile());
                } catch (IOException ioE) {
                    git = Git.init().setDirectory(projectDir.toFile()).call();
                    writeIgnore();
                    System.out.println("gitinited");
                }
            }

            DirCache index = git.add().addFilepattern(".").call();
            Status status = git.status().call();
            System.out.println("status " + status.isClean() + " " + status.hasUncommittedChanges());
            StringBuilder sBuilder = new StringBuilder();
            Set<String> actionMap = new HashSet<>();
            if (!status.isClean() || status.hasUncommittedChanges()) {
                Set<String> addedFiles = status.getAdded();
                for (String addedFile : addedFiles) {
                    String action = "add:" + Paths.get(addedFile).getName(0);
                    actionMap.add(action);
                    System.out.println("added " + addedFile);
                }
                Set<String> changedFiles = status.getChanged();
                for (String changedFile : changedFiles) {
                    String action = "change:" + Paths.get(changedFile).getName(0);
                    actionMap.add(action);
                    System.out.println("changed " + changedFile);
                }
                Set<String> removedFiles = status.getRemoved();
                for (String removedFile : removedFiles) {
                    System.out.println("removed " + removedFile);
                    String action = "remove:" + Paths.get(removedFile).getName(0);
                    actionMap.add(action);
                    git.rm().addFilepattern(removedFile).call();
                }
                Set<String> missingFiles = status.getMissing();
                for (String missingFile : missingFiles) {
                    System.out.println("missing " + missingFile);
                    String action = "missing:" + Paths.get(missingFile).getName(0);
                    actionMap.add(action);
                    git.rm().addFilepattern(missingFile).call();
                }
                actionMap.stream().forEach(action -> sBuilder.append(action).append(","));
                RevCommit commit = git.commit().setMessage(sBuilder.toString()).call();
                didSomething = true;
            }
        } catch (GitAPIException ex) {
            Logger.getLogger(GUIProject.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // fixme, should we do this after each commit, or leave git open
            git.close();
            git = null;
            commitActive = false;
        }
        return didSomething;
    }

    void loadWindows(Path directory) throws IOException {
        Pattern pattern = Pattern.compile("(.+)\\.(yaml)");
        Predicate<String> predicate = pattern.asPredicate();
        final PythonInterpreter interp = MainApp.getInterpreter();
        interp.exec("import nwyaml\\n");
        if (Files.isDirectory(directory)) {
            Files.list(directory).sequential().filter(path -> predicate.test(path.getFileName().toString())).
                    sorted(new Project.FileComparator()).
                    forEach(path -> {
                        String fileName = path.getFileName().toString();
                        Optional<Integer> fileNum = getIndex(fileName);
                        if (fileNum.isPresent()) {
                            interp.exec("nwyaml.loadYamlWin('" + path.toString() + "'" + "," + String.valueOf(fileNum.get()) + ")");
                        }
                    });
        }
    }

    void saveWindows() throws IOException {
        if (projectDir == null) {
            throw new IllegalArgumentException("Project directory not set");
        }
        PythonInterpreter interp = MainApp.getInterpreter();
        int i = 0;
        interp.exec("import nwyaml\\n");
        FXMLController activeController = GUIScripter.getController();
        List<FXMLController> controllers = FXMLController.getControllers();
        for (FXMLController controller : controllers) {
            GUIScripter.setController(controller);
            String fileName = i + "_stage.yaml";
            Path path = Paths.get(projectDir.toString(), "windows", fileName);
            interp.exec("nwyaml.dumpYamlWin('" + path.toString() + "')");
            i++;
        }
        GUIScripter.setController(activeController);
    }

}
