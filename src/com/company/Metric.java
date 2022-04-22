package com.company;

import java.io.*;
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

    public void getData(ArrayList<String> idVersionList, ArrayList<Integer> nbClassList, ArrayList<Float> avgWmcList) throws IOException, InterruptedException {
        // Cloner à local repository
        gitClone();

        // Avoir id_version
        int nbCommits = gitCount();
        getIdVersions(idVersionList, nbCommits);

        // Avoir nombre de classes
        getNbClasse(nbClassList, idVersionList);

        // Avoir moyenne wmc par version
        getWmcAverage(avgWmcList, idVersionList);

    }

    public void gitClone() throws IOException, InterruptedException {
        // Clone projet cible dans repo local
        ProcessBuilder gitClone = new ProcessBuilder().command("git", "clone", link).directory(directory.toFile());
        Process process = gitClone.start();
        try{
            System.out.println("Processing data, this might take a while.");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Wait interrupted.");
        }
        process.destroy();
    }

    public int gitCount() throws IOException {
        // Compte le nombre de commits
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        String temp;
        ProcessBuilder gitCount = new ProcessBuilder().command("git", "rev-list","--count", "master").directory(cloneDirectory.toFile());
        Process processGitCount = gitCount.start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(processGitCount.getInputStream()));
        int nbCommit;
        try {
            nbCommit = Integer.parseInt(stdInput.readLine());
        } catch (IOException e) {
            throw new IOException("Couldn't get count.");
        }
        processGitCount.destroy();
        stdInput.close();
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
        while (boundCount < bound && boundCount < idVersionListTemp.size()) {
            idVersionList.add(idVersionListTemp.get(boundCount));
            boundCount++;
        }
        stdInput.close();
        processRevList.destroy();
    }

    public void getNbClasse(ArrayList<Integer> nbClassList, ArrayList<String> idVersionList) throws IOException {
        // Compte nombre de classe par commit
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        for (String s : idVersionList) {
            ProcessBuilder gitReset = new ProcessBuilder().command("git", "reset","--HARD", s).directory(cloneDirectory.toFile());
            Process processGitReset = gitReset.start();
            nbClassList.add(getFichiersCount(cloneDirectory.toFile()));
            processGitReset.destroy();
        }
    }

    public void getWmcAverage(ArrayList<Float> avgWmcList, ArrayList<String> idVersionList) throws IOException {
        // Compte nombre de classe par commit
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        ArrayList<File> filesList = new ArrayList<>();
        for (String s : idVersionList) {
            ProcessBuilder gitReset = new ProcessBuilder().command("git", "reset","--HARD", s).directory(cloneDirectory.toFile());
            Process processGitReset = gitReset.start();

            getFichiers(dirToFile, filesList);

            ArrayList<Integer> complexityList = new ArrayList<>();
            for (File f : filesList) {
                complexityList.add(getWmc(f.getAbsolutePath()));
            }

            float average = 0;
            for (float i : complexityList) {
                average += i;
            }
            avgWmcList.add(average / complexityList.size());
            processGitReset.destroy();
        }
    }

    /**
     * getWmc calcule la somme pondérée des complexité cyclomatiques de McCabe de toutes les méthodes et classes.
     *
     * @param chemin est la valeur/chemin du fichier a valider
     * @return retourne le nombre de ligne dans le fichier
     */
    public int getWmc(String chemin) {
        //Déclaration des variables
        String ligne = "";
        int noeud = 1;
        int arete = 0;
        File fichierCible = new File(chemin);

        //Vérification si les lignes contiennent les items pour le calcul de WMC et incrémenter les valeurs arete et noeud
        try {
            BufferedReader br = new BufferedReader(new FileReader(fichierCible));
            while ((ligne = br.readLine()) != null) {
                if (ligne.contains("print") || ligne.contains("return")) {
                    arete++;
                    noeud++;
                } else if (ligne.contains("if") ||
                        ligne.contains("else") ||
                        ligne.contains("while") ||
                        ligne.contains("do") ||
                        ligne.contains("for") ||
                        ligne.contains("try") ||
                        ligne.contains("catch")) {
                    try {
                        while (!ligne.contains("}") && !(ligne = br.readLine()).contains("}")) {
                            arete++;
                            noeud++;
                        }
                    } catch (NullPointerException e) {
                        break;
                    }
                    arete++;
                } else if (ligne.contains("switch")) {
                    try {
                        do {
                            ligne = br.readLine();
                            if (!ligne.contains("case")) {
                                arete++;
                                noeud++;
                            }
                        } while (!ligne.contains("default"));

                        while (!ligne.contains("}") && !(ligne = br.readLine()).contains("}")) {
                            arete++;
                            noeud++;
                        }
                    } catch (NullPointerException e) {
                        break;
                    }
                    arete++;
                } else if (ligne.contains("}") &&
                        (ligne.contains("if") ||
                                ligne.contains("else") ||
                                ligne.contains("while") ||
                                ligne.contains("do") ||
                                ligne.contains("for") ||
                                ligne.contains("try") ||
                                ligne.contains("catch"))) {
                    try {
                        while (!ligne.contains("}") && !(ligne = br.readLine()).contains("}")) {
                            arete++;
                            noeud++;
                        }
                    } catch (NullPointerException e) {
                        break;
                    }
                    arete++;
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        return arete - noeud + 2;
    }


    /**
     * Méthode pour aller compter tous les fichiers dans le directory donnée.
     * @param dir directory cible
     */
    public int getFichiersCount(File dir) {
        int count = 0;
        if (dir.isDirectory()) {
            File[] listeFichiers = dir.listFiles();
            for (File f : listeFichiers) {
                count += getFichiersCount(f);
            }
        } else {
            if(dir.getName().endsWith(".java")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Méthode pour aller chercher tous les fichiers dans le directory donnée.
     * @param dir directory cible
     * @param liste liste dans laquelle on met tous les fichiers
     */
    public void getFichiers(File dir, ArrayList<File> liste) {
        if (dir.isDirectory()) {
            File[] listeFichiers = dir.listFiles();
            for (File f : listeFichiers) {
                getFichiers(f, liste);
            }
        } else {
            if(dir.getName().endsWith(".java")) {
                liste.add(dir);
            }
        }
    }
}
