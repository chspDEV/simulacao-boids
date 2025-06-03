package io.github.boids;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends ApplicationAdapter { 
	
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    private List<Boid> boids;
    private static final int INITIAL_AGENT_COUNT = 500;
    private int currentAgentCount = INITIAL_AGENT_COUNT;

    private boolean parallelMode = false;
    private int numThreads;

    private float lastUpdateTimeMillis = 0;
    private String currentModeText = "";
    private String agentCountText = "";
    private String updateTimeText = "";
    private String fpsText = "";

    private float screenWidth;
    private float screenHeight;

    @Override
    public void create() {
        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        font = new BitmapFont(); //para um visual melhor, carregue um .fnt

        boids = new ArrayList<>();
        spawnAgents(INITIAL_AGENT_COUNT); 

        numThreads = Runtime.getRuntime().availableProcessors();
        updateInfoText();

        Gdx.app.log("mainclass", "simulacao iniciada. agentes: " + currentAgentCount);
        Gdx.app.log("mainclass", "num threads para modo paralelo: " + numThreads);
        Gdx.app.log("mainclass", "pressione 'p' para alternar modo sequencial/paralelo."); //controle para alternar entre modo sequencial e paralelo. [cite: 21]
        Gdx.app.log("mainclass", "pressione 'seta para cima' para adicionar 100 agentes.");
        Gdx.app.log("mainclass", "pressione 'seta para baixo' para remover 100 agentes.");
    }

    private void spawnAgents(int count) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            //cada agente deve ter uma posicao e direcao. [cite: 14]
            boids.add(new Boid(random.nextFloat() * screenWidth, random.nextFloat() * screenHeight));
        }
        currentAgentCount = boids.size();
        Gdx.app.log("mainclass", count + " agentes adicionados. total: " + currentAgentCount);
        updateInfoText();
    }

    private void removeAgents(int count) {
        int removedCount = 0;
        for (int i = 0; i < count && !boids.isEmpty(); i++) {
            boids.remove(boids.size() - 1);
            removedCount++;
        }
        currentAgentCount = boids.size();
        if (removedCount > 0) {
            Gdx.app.log("mainclass", removedCount + " agentes removidos. total: " + currentAgentCount);
        }
        updateInfoText();
    }
    
    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            parallelMode = !parallelMode;
            Gdx.app.log("mainclass", "modo alterado para: " + (parallelMode ? "paralelo" : "sequencial"));
            updateInfoText();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            spawnAgents(100); //comece com poucos agentes e va aumentando gradualmente. [cite: 26]
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            removeAgents(100);
        }
    }

    @Override
    public void render() {
        handleInput();
        float deltaTime = Gdx.graphics.getDeltaTime();

        long startTime = System.nanoTime(); //utilize o system.currenttimemillis() para medir tempo de atualizacao por frame. [cite: 26] (nanotime e mais preciso para intervalos curtos)
        if (parallelMode) {
            updateParallel(deltaTime); //versao paralela: dividir o vetor de agentes em chunks e atualizar cada chunk com uma thread separada. [cite: 17]
        } else {
            updateSequential(deltaTime); //versao sequencial: atualizacao de todos os agentes em um unico loop. [cite: 16]
        }
        lastUpdateTimeMillis = (System.nanoTime() - startTime) / 1_000_000f;

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1, true);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.5f, 0.8f, 1f, 1f);
        for (Boid boid : boids) { //agentes visiveis (como circulos ou sprites simples). [cite: 20]
            boid.render(shapeRenderer);
        }
        shapeRenderer.end();

        updateInfoTextWithDynamicValues(deltaTime);
        renderInfo(); //contador de fps ou tempo medio de atualizacao por frame. [cite: 20]
    }

    private void updateSequential(float deltaTime) {
        for (Boid boid : boids) {
            //movimentar-se de acordo com regras estipuladas para o algoritmo de boids. [cite: 15]
            boid.calculateSteeringForces(boids);
        }
        for (Boid boid : boids) {
            boid.applyMotion(deltaTime, screenWidth, screenHeight);
        }
    }

    private void updateParallel(float deltaTime) {
        int totalBoids = boids.size();
        if (totalBoids == 0) return;

        List<Thread> threads = new ArrayList<>(); //threads: java.lang.thread. [cite: 19]
        int chunkSize = (totalBoids + numThreads - 1) / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, totalBoids);
            if (start < end) {
                List<Boid> subList = boids.subList(start, end); //use estruturas de dados simples (como arraylist). [cite: 25]
                //a tarefa principal sera paralelizar a atualizacao do estado dos agentes sem causar concorrencia de dados cada thread lidara com um subconjunto isolado dos agentes. [cite: 13]
                Thread t = new Thread(new AgentUpdateTask(subList, boids, deltaTime, screenWidth, screenHeight));
                threads.add(t);
                t.start();
            }
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Gdx.app.error("mainclass", "thread de atualizacao interrompida", e);
            }
        }
        //a atualizacao paralela deve evitar qualquer tipo de escrita concorrente. [cite: 18]
        //cada thread atualiza apenas os agentes do seu chunk, e os agentes nao compartilham estado (durante a escrita). [cite: 18]
        //certifique-se de que cada thread so acessa seus proprios dados (para escrita de estado final). [cite: 25]
    }
    
    private void updateInfoText() {
        currentModeText = "modo: " + (parallelMode ? "paralelo (" + numThreads + "t)" : "sequencial");
        agentCountText = "agentes: " + currentAgentCount;
    }

    private void updateInfoTextWithDynamicValues(float deltaTime){
        updateTimeText = String.format("update: %.2f ms", lastUpdateTimeMillis);
        fpsText = "fps: " + Gdx.graphics.getFramesPerSecond();
    }

    private void renderInfo() {
        spriteBatch.begin();
        font.draw(spriteBatch, currentModeText, 10, screenHeight - 20);
        font.draw(spriteBatch, agentCountText, 10, screenHeight - 40);
        font.draw(spriteBatch, updateTimeText, 10, screenHeight - 60);
        font.draw(spriteBatch, fpsText, 10, screenHeight - 80);
        spriteBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        if (spriteBatch != null && spriteBatch.getProjectionMatrix() != null) { //previne nullpointer se resize for chamado antes de create completo
            spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        }
        if (shapeRenderer != null && shapeRenderer.getProjectionMatrix() != null) {
            shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        }
        Gdx.app.log("mainclass", "tela redimensionada para: " + width + "x" + height);
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (font != null) font.dispose();
        Gdx.app.log("mainclass", "simulacao encerrada e recursos liberados.");
    }
}