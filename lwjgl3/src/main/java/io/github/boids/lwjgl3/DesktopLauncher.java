package io.github.boids.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.boids.Main;

public class DesktopLauncher {
	public static void main (String[] arg) {
		//configuracao da aplicacao lwjgl3 (backend desktop mais moderno)
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

		//define o titulo da janela da aplicacao
		config.setTitle("Simulacao de Enxame Boids");

		//define a taxa de quadros por segundo (fps) desejada
		config.setForegroundFPS(60);

		//define as dimensoes iniciais da janela
		//voce pode ajustar esses valores conforme necessario
		config.setWindowedMode(1024, 768); //largura e altura iniciais


		//cria a aplicacao, passando sua classe principal (main) e a configuracao
		new Lwjgl3Application(new Main(), config);
	}
}