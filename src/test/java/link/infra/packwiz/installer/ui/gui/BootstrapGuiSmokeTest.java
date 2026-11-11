package link.infra.packwiz.installer.ui.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.AbstractButton;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

/** Runs without installer dependencies on the JVM class path, just like the bootstrapper. */
public final class BootstrapGuiSmokeTest {
	public static void main(String[] args) throws Exception {
		if (args.length > 1) {
			try (JarInputStream jar = new JarInputStream(new FileInputStream(args[0]))) {
				String version = jar.getManifest().getMainAttributes().getValue("Implementation-Version");
				if (!args[1].equals(version))
					throw new AssertionError("Expected installer version " + args[1] + ", got " + version);
			}
			System.out.println("Packaged installer version matches " + args[1]);
		}
		if (GraphicsEnvironment.isHeadless()) {
			System.out.println("SKIPPED bootstrap GUI smoke test: a display is required (use xvfb-run).");
			return;
		}
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		PrintStream originalError = System.err;
		try (URLClassLoader installer = new URLClassLoader(
				new URL[] { new File(args[0]).toURI().toURL() }, BootstrapGuiSmokeTest.class.getClassLoader())) {
			// Initialize Swing before loading the installer; its EDT cannot see the child JAR.
			EventQueue.invokeAndWait(() -> {});
			System.setErr(new PrintStream(errors, true, "UTF-8"));
			Class<?> handlerClass = installer.loadClass("link.infra.packwiz.installer.ui.gui.GUIHandler");
			Object handler = handlerClass.getConstructor().newInstance();
			try {
				Field field = handlerClass.getDeclaredField("frmPackwizlauncher");
				field.setAccessible(true);
				Window window = (Window) field.get(handler);
				EventQueue.invokeAndWait(() -> {
					if (!UIManager.getLookAndFeel().getClass().getName().contains("FlatDarkLaf"))
						throw new AssertionError("The dark theme was not installed");
					assertComponents(window);
					if (!new JPanel().getUI().getClass().getName().startsWith("com.formdev.flatlaf."))
						throw new AssertionError("Swing did not resolve the child-loaded UI delegate");
					window.setVisible(true);
				});
			} finally {
				handlerClass.getMethod("dispose").invoke(handler);
			}
			if (errors.size() != 0)
				throw new AssertionError("GUI initialization wrote errors: " + errors.toString("UTF-8"));
			System.out.println("Bootstrapper-style packaged GUI smoke test passed.");
		} finally {
			System.setErr(originalError);
			originalError.print(errors.toString("UTF-8"));
			EventQueue.invokeAndWait(() -> {
				for (Window window : Window.getWindows()) window.dispose();
			});
		}
	}

	private static void assertComponents(Component component) {
		if ((component instanceof JPanel && ((JPanel) component).getUI() == null) ||
				(component instanceof JLabel && ((JLabel) component).getUI() == null) ||
				(component instanceof AbstractButton && ((AbstractButton) component).getUI() == null) ||
				(component instanceof JProgressBar && ((JProgressBar) component).getUI() == null))
			throw new AssertionError("Missing UI delegate: " + component.getClass().getName());
		if (component instanceof JLabel && component.getFont() == null)
			throw new AssertionError("Missing label font");
		if (component instanceof Container)
			for (Component child : ((Container) component).getComponents()) assertComponents(child);
	}
}
