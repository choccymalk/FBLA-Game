public class FBLAGame {
    public static void main(String[] args) {
        Window window = new Window(800, 800, "Spinning Multicolored Cube");
        Renderer renderer = new Renderer();

        window.show();

        while (!window.shouldClose()) {
            renderer.render(window);
            window.update();
        }

        renderer.cleanup();
        window.cleanup();
    }
}