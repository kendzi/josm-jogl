/*
 * This software is provided "AS IS" without a warranty of any kind. You use it
 * on your own risk and responsibility!!! This file is shared under BSD v3
 * license. See readme.txt and BSD3 file for details.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class registering JOGL libraries in JOSM classLoader.
 *
 * @author Tomasz Kędziora (Kendzi)
 */
public class JoglPlugin extends Plugin {

    /**
     * Jogl libraries directory.
     */
    private static final String JOGL_LIB_DIR = "/lib/jogl-${joglVersion}/";

    /**
     * Jogl loader instance.
     */
    private static JoglPlugin joglPlugin;

    private String joglVersion;

    /**
     * Add JOGL libraries to JOSM classpath.
     */
    public static void addJoglToClassPath() {
        try {
            getInstance().addLibraryToClassPath();
        } catch (Exception e) {
            throw new RuntimeException("can't add jogl libs to classpath", e);
        }
    }

    /**
     * Get Jogl loader instance. Instance is setup after plug-in is loaded.
     *
     * @return jogl loader
     */
    private static JoglPlugin getInstance() {
        if (joglPlugin == null) {
            throw new RuntimeException("Plugin JoglPlugin has not been started yet");
        }
        return joglPlugin;
    }

    /**
     * Setup instance.
     *
     * @param joglPlugin
     */
    private static void setInstance(JoglPlugin joglPlugin) {
        if (JoglPlugin.joglPlugin != null) {
            throw new RuntimeException("this plugin is library and it can be loaded only once!");
        }
        JoglPlugin.joglPlugin = joglPlugin;
        joglPlugin.loadJoglVersion();
    }

    private void loadJoglVersion() {
        Properties prop = new Properties();
        try {
            InputStream in = getClass().getResourceAsStream("/josm-jogl.properties");
            prop.load(in);
            in.close();

            joglVersion = prop.getProperty("jogl.version");

            if (joglVersion == null) {
                throw new IllegalStateException("can't load jogl version from properties file");
            }
        } catch (IOException e) {
            throw new IllegalStateException("can't load jogl version!");
        }

    }

    /**
     * Constructor.
     *
     * @param info
     *            plug-in info
     */
    public JoglPlugin(PluginInformation info) {
        super(info);

        setInstance(this);
    }

    private void addLibraryToClassPath()
            throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            SecurityException, ClassNotFoundException {

        List<String> liblaryNamesList = getJoglLibs();

        copyFilesFromJar(liblaryNamesList);

        addJarsToClassLoader(liblaryNamesList);
    }

    private List<String> getJoglLibs() {

        List<String> libNames = Arrays.asList( //
                "jogl-all-${joglVersion}-natives-linux-armv6.jar", //
                "jogl-all-${joglVersion}-natives-linux-armv6hf.jar", //
                "jogl-all-${joglVersion}-natives-linux-amd64.jar", //
                "jogl-all-${joglVersion}-natives-linux-i586.jar", //
                "jogl-all-${joglVersion}-natives-macosx-universal.jar", //
                "jogl-all-${joglVersion}-natives-solaris-amd64.jar", //
                "jogl-all-${joglVersion}-natives-solaris-i586.jar", //
                "jogl-all-${joglVersion}-natives-windows-amd64.jar", //
                "jogl-all-${joglVersion}-natives-windows-i586.jar", //
                "jogl-all-${joglVersion}.jar", //
                "gluegen-rt-${joglVersion}-natives-linux-armv6.jar", //
                "gluegen-rt-${joglVersion}-natives-linux-armv6hf.jar", //
                "gluegen-rt-${joglVersion}-natives-linux-amd64.jar", //
                "gluegen-rt-${joglVersion}-natives-linux-i586.jar", //
                "gluegen-rt-${joglVersion}-natives-macosx-universal.jar", //
                "gluegen-rt-${joglVersion}-natives-solaris-amd64.jar", //
                "gluegen-rt-${joglVersion}-natives-solaris-i586.jar", //
                "gluegen-rt-${joglVersion}-natives-windows-amd64.jar", //
                "gluegen-rt-${joglVersion}-natives-windows-i586.jar", //
                "gluegen-rt-${joglVersion}.jar"//
        );

        List<String> ret = new ArrayList<String>();
        for (String libName : libNames) {
            if (!libName.contains("natives")
                    || (Main.isPlatformWindows() && libName.contains("windows"))
                    || (Main.isPlatformOsx() && libName.contains("macosx"))
                    || (Main.platform instanceof PlatformHookUnixoid
                            && (libName.contains("linux") || libName.contains("solaris")))) {
                ret.add((JOGL_LIB_DIR + libName).replaceAll("\\$\\{joglVersion\\}", joglVersion));
            }
        }
        return ret;
    }

    /**
     * Registering external jars to ClassLoader.
     *
     * @param pLibraryNamesList
     *            list of jars
     *
     * @throws NoSuchMethodException
     *             ups
     * @throws IllegalAccessException
     *             ups
     * @throws InvocationTargetException
     *             ups
     * @throws MalformedURLException
     *             ups
     * @throws ClassNotFoundException
     *             ups
     * @throws SecurityException
     *             ups
     */
    private void addJarsToClassLoader(List<String> pLibraryNamesList)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, MalformedURLException,
            SecurityException, ClassNotFoundException {

        ClassLoader sysLoader = Main.class.getClassLoader();

        // try to load jars and dll
        if (sysLoader instanceof URLClassLoader) {
            // Up to Java 8 the system class loader is an instance of java.net.URLClassLoader
            doAddJarsToClassLoader(pLibraryNamesList, sysLoader,
                    URLClassLoader.class.getDeclaredMethod("addURL", URL.class),
                        f -> {
                            try {
                                return f.toURI().toURL();
                            } catch (MalformedURLException e) {
                                throw new JosmRuntimeException(e);
                            }
                        });
        } else if (sysLoader != null) {
            // Starting from Java 9 the system class loader is an instance of jdk.internal.ClassLoaders.AppClassLoader
            doAddJarsToClassLoader(pLibraryNamesList, sysLoader,
                    Class.forName("jdk.internal.loader.ClassLoaders$AppClassLoader")
                        .getDeclaredMethod("appendToClassPathForInstrumentation", String.class),
                        f -> f.getAbsolutePath());
        }
    }

    private void doAddJarsToClassLoader(List<String> pLibraryNamesList, ClassLoader pSysLoader, Method pMethod,
            Function<File, Object> pArgBuilder)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, MalformedURLException {
        Utils.setObjectsAccessible(pMethod);
        for (String libName : pLibraryNamesList) {
            File library = new File(getPluginDir(), libName);
            if (library.exists()) {
                System.out.println("loading lib: " + library.getAbsoluteFile());
            } else {
                System.err.println("lib don't exist!: " + library.getAbsoluteFile());
            }
            pMethod.invoke(pSysLoader, pArgBuilder.apply(library));
        }
    }

    private String getPluginDir() {
		return getPluginDirs().getUserDataDirectory(false).getPath();
	}

	/**
     * Coping file list from jar to plug-in directory.
     *
     * @param pFilesPathList
     *            list of files path in jar
     *
     * @throws IOException
     *             ups
     */
    private void copyFilesFromJar(List<String> pFilesPathList) throws IOException {

        for (String from : pFilesPathList) {
            copy(from, true);
        }
    }

    /**
     * Make all sub directories.
     *
     * @param to
     *            file which require directory
     */
    private void makeParentDirs(String to) {
        File parentDir = getParentDir(to);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    /**
     * Gets resource url.
     *
     * @param resourceName
     *            the resource name
     * @return the url
     */
    private URL getResourceUrl(String resourceName) {

        if (resourceName != null) {
            resourceName = resourceName.trim();
            if (resourceName.startsWith("/")) {
                resourceName = resourceName.substring(1);
            }
        }

        return getPluginResourceClassLoader().getResource(resourceName);
    }

    private File getParentDir(String from) {
        return new File(getPluginDir(), from).getParentFile();
    }

    /**
     * Check if file exist in plug-in directory.
     *
     * @param pFileName
     *            file path
     * @return if file exist
     */
    private boolean isFileExis(String pFileName) {
        File file = new File(getPluginDir(), pFileName);
        return file.exists() && file.length() != 0;
    }

    private void copy(String from, boolean skipIfExist) throws IOException {
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
            status += "end of copying bytes: " + l + " from file: " + from + " at url: " + fromUrl;

        } finally {
            System.out.println(status);
        }
    }
}
