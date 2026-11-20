package link.infra.packwiz.installer.ui.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.JFrame;
import javax.swing.JDialog;
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
				if (window.getIconImages().isEmpty())
					throw new AssertionError("The bundled window logo is missing");
				EventQueue.invokeAndWait(() -> {
					if (!UIManager.getLookAndFeel().getClass().getName().contains("FlatDarkLaf"))
						throw new AssertionError("The dark theme was not installed");
					assertComponents(window);
					if (!new JPanel().getUI().getClass().getName().startsWith("com.formdev.flatlaf."))
						throw new AssertionError("Swing did not resolve the child-loaded UI delegate");
					window.setVisible(true);
				});
				// The WebP plugin must work in the final JAR without being visible
				// to Swing's or ImageIO's parent context class loader.
				ByteArrayOutputStream webp = new ByteArrayOutputStream();
				try (InputStream input = BootstrapGuiSmokeTest.class.getResourceAsStream("/icons/test.webp")) {
					if (input == null) throw new AssertionError("Missing WebP test fixture");
					byte[] buffer = new byte[1024];
					int read;
					while ((read = input.read(buffer)) != -1) webp.write(buffer, 0, read);
				}
				Object accepted = handlerClass.getMethod("setPackIcon", byte[].class).invoke(handler, webp.toByteArray());
				if (!Boolean.TRUE.equals(accepted))
					throw new AssertionError("The packaged WebP decoder could not read a valid image");
				EventQueue.invokeAndWait(() -> {
					if (window.getIconImages().get(0).getWidth(null) != 16)
						throw new AssertionError("The decoded WebP was not applied to the window");
				});
				verifyGroupedOptions(installer, (JFrame) window);
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

	private static void verifyGroupedOptions(ClassLoader loader, JFrame parent) throws Exception {
		Class<?> optionType = loader.loadClass("link.infra.packwiz.installer.ui.data.IOptionDetails");
		boolean[] enabled = { false, false };
		Object[] choices = new Object[2];
		for (int index = 0; index < choices.length; index++) {
			final int i = index;
			choices[i] = Proxy.newProxyInstance(loader, new Class<?>[] { optionType }, (proxy, method, args) -> {
				switch (method.getName()) {
					case "getName": return "VR test " + i;
					case "getOptionDescription": return "VR description";
					case "getOptionValue": return enabled[i];
					case "setOptionValue": enabled[i] = (Boolean) args[0]; return null;
					case "hashCode": return System.identityHashCode(proxy);
					case "equals": return proxy == args[0];
					case "toString": return "VR test " + i;
					default: throw new AssertionError("Unexpected option method: " + method);
				}
			});
		}
		List<Object> options = Arrays.asList(choices);
		Object group = loader.loadClass("link.infra.packwiz.installer.ui.data.OptionGroup")
				.getConstructor(String.class, String.class, String.class, List.class)
				.newInstance("vr", "VR Support", "Test group", options);
		Class<?> dialogType = loader.loadClass("link.infra.packwiz.installer.ui.gui.OptionsSelectWindow");
		EventQueue.invokeAndWait(() -> {
			try {
				CompletableFuture<Boolean> result = new CompletableFuture<>();
				JDialog dialog = (JDialog) dialogType
						.getConstructor(List.class, CompletableFuture.class, JFrame.class, List.class)
						.newInstance(options, result, parent, Collections.singletonList(group));
				try {
					assertComponents(dialog);
					AbstractButton groupButton = findButton(dialog, "VR Support (");
					if (groupButton == null) throw new AssertionError("Grouped selection is missing");
					groupButton.doClick();
					if (enabled[0] || enabled[1]) throw new AssertionError("Choices changed before Save");
					findButton(dialog, "Save selection").doClick();
					if (!enabled[0] || !enabled[1] || result.getNow(true))
						throw new AssertionError("Grouped choices were not saved");
				} finally { dialog.dispose(); }
			} catch (ReflectiveOperationException e) { throw new AssertionError(e); }
		});
	}

	private static AbstractButton findButton(Component component, String prefix) {
		if (component instanceof AbstractButton) {
			AbstractButton button = (AbstractButton) component;
			if (button.getText() != null && button.getText().startsWith(prefix)) return button;
		}
		if (component instanceof Container)
			for (Component child : ((Container) component).getComponents()) {
				AbstractButton result = findButton(child, prefix);
				if (result != null) return result;
			}
		return null;
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
