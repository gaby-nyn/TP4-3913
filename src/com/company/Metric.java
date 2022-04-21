package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

public class Metric {
    private String link;
    private Path directory;
    private File dirToFile;

    public Metric(Path directory, String link) {
        this.directory = directory;
        this.dirToFile = directory.toFile();
        this.link = link;
    }

    public void getData(ArrayList<String> idVersionList, ArrayList<Integer> nbClassList) throws IOException, InterruptedException {
        gitClone();
        int nbCommits = gitCount();
        getIdVersions(idVersionList, nbCommits);
        getNbClasse(nbClassList, idVersionList);
    }

    public void gitClone() throws IOException, InterruptedException {
        // Clone projet cible dans repo local
        ProcessBuilder gitClone = new ProcessBuilder().command("git", "clone", link).directory(directory.toFile());
        Process process = gitClone.start();
        try{
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Wait interrupted.");
        }
    }

    public int gitCount() throws IOException {
        // Compte le nombre de commits
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        String temp;
        ProcessBuilder gitCount = new ProcessBuilder().command("git", "rev-list","--count", "master").directory(cloneDirectory.toFile());
        Process processGitCount = gitCount.start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(processGitCount.getInputStream()));
        int nbCommit = 0;
        try {
            nbCommit = Integer.parseInt(stdInput.readLine());
        } catch (IOException e) {
            throw new IOException("Couldn't get count.");
        }
        return nbCommit;
    }

    public void getIdVersions(ArrayList<String> idVersionList, int nbCommit) throws IOException {
        // Prend les IDs du repo cible
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        ProcessBuilder gitRevList = new ProcessBuilder().command("git", "rev-list", "master").directory(cloneDirectory.toFile());
        Process processRevList = gitRevList.start();

        // Arraylist contenant tous les IDs
        String temp;
        ArrayList<String> idVersionListTemp = new ArrayList<>();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(processRevList.getInputStream()));
        while ((temp = stdInput.readLine()) != null) {
            idVersionListTemp.add(temp);
        }

        // Prend aléatoirement 5% des versions
        Collections.shuffle(idVersionListTemp);
        int bound = (int)(nbCommit*(5.0f/100.0f));
        int boundCount = 0;
        while (boundCount < bound) {
            idVersionList.add(idVersionListTemp.get(boundCount));
            boundCount++;
        }
    }

    public void getNbClasse(ArrayList<Integer> nbClassList, ArrayList<String> idVersionList) throws IOException {
        // Compte nombre de classe par commit
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        for (String s : idVersionList) {
            ProcessBuilder gitReset = new ProcessBuilder().command("git", "reset","--HARD", s).directory(cloneDirectory.toFile());
            Process processGitReset = gitReset.start();
            nbClassList.add(getFichiers(cloneDirectory.toFile()));
        }
    }


    /**
     * Méthode pour aller chercher tous les fichiers dans le directory donnée.
     * @param dir directory cible
     */
    public int getFichiers(File dir) {
        int count = 0;
        if (dir.isDirectory()) {
            File[] listeFichiers = dir.listFiles();
            for (File f : listeFichiers) {
                count += getFichiers(f);
            }
        } else {
            if(dir.getName().endsWith(".java")) {
                count++;
            }
        }
        return count;
    }
}
