/*
 * This software is provided "AS IS" without a warranty of any kind.
 * You use it on your own risk and responsibility!!!
 *
 * This file is shared under BSD v3 license.
 * See readme.txt and BSD3 file for details.
 *
 */
package kendzi.josm.jogl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * This class registering JOGL libraries in JOSM classLoader.
 * 
 * @author Tomasz Kędziora (Kendzi)
 */
public class JoglPlugin extends Plugin {

    /**
     * Jogl loader instance.
     */
    private static JoglPlugin joglPlugin;

    /**
     * Add JOGL libraries to JOSM classpath.
     */
    public static void addJoglToClassPath() {
        try {
            getInstance().addLiblaryToClassPath();
        } catch (Exception e) {
            throw new RuntimeException("can't add jogl libs to classpath", e);
        }
    }

    /**
     * Get Jogl loader instance. Instance is setup after plugin is loaded.
     * @return jogl loader
     */
    private static JoglPlugin getInstance() {
        if (joglPlugin == null) {
            throw new RuntimeException("Plugin JoglPlugin has not been started yet");
        }
        return joglPlugin;
    }

    /** Setup instance.
     * @param pJoglPlugin
     */
    private static void setInstance(JoglPlugin pJoglPlugin) {
        if (joglPlugin != null) {
            throw new RuntimeException("this plugin is liblary and it can be loaded only once!");
        }
        joglPlugin = pJoglPlugin;
    }

    /** Constructor.
     * @param info plug-in info
     */
    public JoglPlugin(PluginInformation info) {
        super(info);

        setInstance(this);
    }

    private void addLiblaryToClassPath() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        List<String> liblaryNamesList = getJoglLibs();

        copyFilesFromJar(liblaryNamesList);

        addJarsToClassLoader(liblaryNamesList);
    }

    private List<String> getJoglLibs() {

        return Arrays.asList(
                "/lib/jogl-v2.0-rc11/jogl-all-natives-linux-amd64.jar",
                "/lib/jogl-v2.0-rc11/jogl-all-natives-linux-i586.jar",
                "/lib/jogl-v2.0-rc11/jogl-all-natives-macosx-universal.jar",
                "/lib/jogl-v2.0-rc11/jogl-all-natives-solaris-amd64.jar",
                "/lib/jogl-v2.0-rc11/jogl-all-natives-solaris-i586.jar",
                "/lib/jogl-v2.0-rc11/jogl-all-natives-windows-amd64.jar",
                "/lib/jogl-v2.0-rc11/jogl-all-natives-windows-i586.jar",
                "/lib/jogl-v2.0-rc11/jogl-all.jar",

                "/lib/jogl-v2.0-rc11/gluegen-rt-natives-linux-amd64.jar",
                "/lib/jogl-v2.0-rc11/gluegen-rt-natives-linux-i586.jar",
                "/lib/jogl-v2.0-rc11/gluegen-rt-natives-macosx-universal.jar",
                "/lib/jogl-v2.0-rc11/gluegen-rt-natives-solaris-amd64.jar",
                "/lib/jogl-v2.0-rc11/gluegen-rt-natives-solaris-i586.jar",
                "/lib/jogl-v2.0-rc11/gluegen-rt-natives-windows-amd64.jar",
                "/lib/jogl-v2.0-rc11/gluegen-rt-natives-windows-i586.jar",
                "/lib/jogl-v2.0-rc11/gluegen-rt.jar"
                );
    }

    /**
     * Registering external jars to ClassLoader.
     *
     * @param pLiblaryNamesList list of jars
     *
     * @throws NoSuchMethodException ups
     * @throws IllegalAccessException ups
     * @throws InvocationTargetException ups
     * @throws MalformedURLException ups
     */
    private void addJarsToClassLoader(List<String> pLiblaryNamesList)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, MalformedURLException {

        URLClassLoader sysLoader = (URLClassLoader) Main.class.getClassLoader();

        // try to load jars and dll
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        Method method = sysclass.getDeclaredMethod("addURL",
                new Class[] { URL.class });
        method.setAccessible(true);

        for (int i = 0; i < pLiblaryNamesList.size(); i++) {
            File library = new File(getPluginDir() + "/" + pLiblaryNamesList.get(i));
            if (library.exists()) {
                System.out.println("loading lib: " + library.getAbsoluteFile());
            } else {
                System.err.println("lib don't exist!: " + library.getAbsoluteFile());
            }
            method.invoke(sysLoader, new Object[] { library.toURI().toURL() });
        }
    }

    /** Coping file list from jar to plugin dir.
     * @param pFilesPathList list of files path in jar
     *
     * @throws IOException ups
     */
    private void copyFilesFromJar(List<String> pFilesPathList) throws IOException {

        for (String from : pFilesPathList) {
            copy(from, true);
        }
    }


    /**
     * Make all sub directories.
     * @param to file which require directory
     */
    private void makeParentDirs(String to) {
        File parentDir = getParentDir(to);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    public URL getResourceUrl(String pResName) {

        if (pResName != null) {
            pResName = pResName.trim();
            if (pResName.startsWith("/")) {
                pResName = pResName.substring(1);
            }
        }

        return getPluginResourceClassLoader().getResource(pResName);
    }

    private File getParentDir(String from) {
        return new File(getPluginDir(), from).getParentFile();
    }

    /**
     * Check if file exist in plugin directory.
     * @param pFileName file path
     * @return if file exist
     */
    public boolean isFileExis(String pFileName) {
        File file = new File(getPluginDir(), pFileName);
        return file.exists() && file.length() != 0;
    }

    public void copy(String from, boolean skipIfExist) throws IOException {
        String status = "";

        try {

            String to = from;
            if (skipIfExist && isFileExis(to)) {
                status += "copying file: " + from + " exist skiping\n";
                return;
            }

            status += "starting copying file from jar to plugin dir " + from + "\n";

            makeParentDirs(to);


            URL fromUrl = getResourceUrl(from);
            if (fromUrl == null) {
                throw new IOException("Can't get url for from location: " + from);
            }

            status += "fromUrl in plugin jar: " + fromUrl + "\n";

            InputStream in = null;
            try {
                in = fromUrl.openStream();
            } catch (IOException e) {
                throw new IOException("Can't open stream to resource: " + fromUrl, e);
            }

            FileOutputStream out = new FileOutputStream(new File(getPluginDir(), to));

            status += "starting copying \n";

            byte[] buffer = new byte[8192];
            long l = 0;
            for (int len = in.read(buffer); len > 0; len = in.read(buffer)) {
                out.write(buffer, 0, len);
                l = l + len;
            }

            in.close();
            out.close();
            status +="end of copying bytes: " + l + " from file: " + from + " at url: " + fromUrl;

        } finally {
            System.out.println(status);
        }
    }
}
