package com.company;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

public class Metric {
    private String link;
    private Path directory;

    public Metric(Path directory, String link) {
        this.directory = directory;
        this.link = link;
    }

    public void getData(ArrayList<String> idVersionList, ArrayList<Integer> nbClassList, ArrayList<Float> avgWmcList, ArrayList<Float> avgBcList) throws IOException, InterruptedException {
        // Cloner à local repository
        gitClone();

        // Avoir id_version
        int nbCommits = gitCount();
        getIdVersions(idVersionList, nbCommits);

        // Avoir nombre de classes
        getNbClasse(nbClassList, idVersionList);

        // Avoir moyennes par version
        getAverages(avgWmcList, idVersionList, avgBcList);

    }

    public void gitClone() throws IOException, InterruptedException {
        // Clone projet cible dans repo local
        ProcessBuilder gitClone = new ProcessBuilder().command("git", "clone", link).directory(directory.toFile());
        Process process = gitClone.start();
        process.waitFor();
            System.out.println("Processing data, this might take a while.");
        process.destroy();
    }

    public int gitCount() throws IOException, InterruptedException {
        // Compte le nombre de commits
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        ProcessBuilder gitCount = new ProcessBuilder().command("git", "rev-list","--count", "master").directory(cloneDirectory.toFile());
        Process processGitCount = gitCount.start();
        processGitCount.waitFor();
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

    public void getIdVersions(ArrayList<String> idVersionList, int nbCommit) throws IOException, InterruptedException {
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

    public void getNbClasse(ArrayList<Integer> nbClassList, ArrayList<String> idVersionList) throws IOException, InterruptedException {
        // Compte nombre de classe par commit
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        for (String s : idVersionList) {
            ProcessBuilder gitReset = new ProcessBuilder().command("git", "reset","--hard", s).directory(cloneDirectory.toFile());
            Process processGitReset = gitReset.start();
            processGitReset.waitFor();
            nbClassList.add(getFichiersCount(cloneDirectory.toFile()));
            processGitReset.destroy();
        }
    }

    public void getAverages(ArrayList<Float> avgWmcList, ArrayList<String> idVersionList, ArrayList<Float> avgBcList) throws IOException, InterruptedException {
        File[] files = directory.toFile().listFiles();
        Path cloneDirectory = files[0].toPath();
        for (String s : idVersionList) {
            ArrayList<File> filesList = new ArrayList<>();
            ProcessBuilder gitWmcAvg = new ProcessBuilder().command("git", "reset","--hard", s).directory(cloneDirectory.toFile());
            Process processGitWmcAvg = gitWmcAvg.start();
            processGitWmcAvg.waitFor();

            getFichiers(cloneDirectory.toFile(), filesList);

            ArrayList<Integer> complexityList = new ArrayList<>();
            ArrayList<Float> bcList = new ArrayList<>();
            for (File f : filesList) {
                if (!f.getAbsolutePath().isEmpty()) {
                    int wmc = getWmc(f.getAbsolutePath());
                    int cloc = getCloc(f.getAbsolutePath());
                    int loc = getLoc(f.getAbsolutePath());

                    if (wmc != 0 && loc != 0 && cloc != 0) {
                        float dc = (float) cloc / loc;
                        bcList.add(dc/wmc);
                    }
                    if (wmc != 0) {
                        complexityList.add(wmc);
                    }
                }
            }

            float average = 0;
            for (float i : complexityList) {
                average += i;
            }
            avgWmcList.add(average / complexityList.size());
            average = 0;
            for (float i : bcList) {
                average += i;
            }
            avgBcList.add(average / bcList.size());
            processGitWmcAvg.destroy();
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
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        return arete - noeud + 2;
    }

    /**
     * getCloc sert à compter le nombre de ligne de commentaire dans le fichier.
     *
     * @param chemin est la valeur/chemin du fichier a valider
     * @return retourne le nombre de ligne dans le fichier
     */
    public int getCloc(String chemin) {
        String ligne = "";
        int nombreLignesCommentaire = 0;
        File fichierCible = new File(chemin);

        //Vérifier les séparateurs de commentaires et calculer le nombre de ligne
        try {
            BufferedReader br = new BufferedReader(new FileReader(fichierCible));
            while ((ligne = br.readLine()) != null) {
                if (ligne.contains("//")) {
                    nombreLignesCommentaire++;
                } else if (ligne.contains("/*")) {
                    while (!ligne.contains("*/") && !(ligne = br.readLine()).contains("*/")) {
                        nombreLignesCommentaire++;
                    }
                } else if (ligne.contains("/**")) {
                    while (!ligne.contains("*/") && !(ligne = br.readLine()).contains("*/")) {
                        nombreLignesCommentaire++;
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            return 0;
        } catch (NoSuchFileException e) {
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        return nombreLignesCommentaire;
    }

    /**
     * getLoc est utilisée pour compter le nombre de ligne de code dans un fichier et retourne ce nombre.
     *
     * @param chemin est la valeur/chemin du fichier a valider
     * @return retourne le nombre de ligne dans le fichier
     */
    public int getLoc(String chemin) {
        int nombreLignesCode = 0;
        File fichierCible = new File(chemin);
        Path pathChemin = Paths.get(fichierCible.getAbsolutePath());

        //Compter le nombre de ligne dans le fichier ou retourner 0 si erreur
        try {
            Stream<String> stream = Files.lines(pathChemin, StandardCharsets.ISO_8859_1);
            nombreLignesCode += stream.count();
            stream.close();
        } catch (FileNotFoundException e) {
            return 0;
        } catch (NoSuchFileException e) {
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        return nombreLignesCode;
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
