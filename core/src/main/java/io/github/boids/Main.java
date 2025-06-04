package io.github.boids;

//aqui a gente importa tudo que o libgdx oferece e que vamos usar
//desde coisas basicas de aplicacao ate graficos e interface de usuario
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable; //voce tinha esse import, mantendo
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

//bibliotecas padrao do java que podem ser uteis
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//essa e a nossa classe principal, o coracao da simulacao, sacou?
//ela herda de applicationadapter, que e tipo o molde basico pra um app/jogo libgdx
public class Main extends ApplicationAdapter {
    //o spritebatch e quem desenha nossas imagens na tela de forma eficiente
    private SpriteBatch loteDeSprites;
    //a fonte e pra gente poder escrever texto na tela, tipo infos de debug ou ui
    private BitmapFont fonte;

    //aqui guardamos o arquivo de textura do boid que carregamos da pasta assets
    private Texture arquivoTexturaBoid;
    //textura fundo
    private Texture arquivoTexturaFundo;
    //e este e o sprite especifico do boid e fundo, recortado da textura se necessario
    private TextureRegion spriteBoid, spriteFundo;

    //uma lista pra guardar todos os nossos agentes (boids)
    private List<Boid> agentesLista;
    //aqui a gente define quantos agentes comecam na tela, tipo um valor padrao [cite: 2]
    private static final int CONTAGEM_INICIAL_AGENTES = 50;
    //e esta variavel guarda quantos agentes temos no momento, comecando com o valor inicial
    private int contagemAtualAgentes = CONTAGEM_INICIAL_AGENTES;

    //um booleano pra gente saber se estamos no modo de processamento paralelo ou nao
    private boolean modoParalelo = false;
    //quantas threads nosso processador tem? guardamos aqui pra usar no modo paralelo
    private int numeroDeThreads;
    //essa variavel controla a velocidade geral da simulacao, tipo um replay mais rapido ou lento
    private float velocidadeSimulacao = 1.0f; //1.0f e a velocidade normal

    //pra gente medir o tempo que cada atualizacao de frame leva, em milissegundos
    private float ultimoTempoAtualizacaoMillis = 0;
    //textos que vao aparecer na tela pra informar o usuario
    private String textoModoAtual = ""; //mostra se ta paralelo ou sequencial
    private String textoTempoAtualizacao = ""; //mostra o tempo do ultimo update
    private String textoFps = ""; //mostra os frames por segundo

    //guardamos a largura e altura da tela pra usar nos calculos de posicao e bordas
    private float larguraTela;
    private float alturaTela;

    //coisas do scene2d.ui pra nossa interface grafica de debug
    private Stage palco;
    private Skin skinUI;
    private Table tabelaUI;
    private Label rotuloContagemAgentesUI;
    private Label rotuloVelocidadeSimulacaoUI;
    //novo: label para o slider de peso de coesao
    private Label rotuloPesoCoesaoUI;


    @Override
    public void create() {
        larguraTela = Gdx.graphics.getWidth();
        alturaTela = Gdx.graphics.getHeight();

        loteDeSprites = new SpriteBatch();
        fonte = new BitmapFont();

        try {
            arquivoTexturaBoid = new Texture(Gdx.files.internal("boid_sprite.png"));
            spriteBoid = new TextureRegion(arquivoTexturaBoid);
            
            arquivoTexturaFundo = new Texture(Gdx.files.internal("fundo.png")); //carregando fundo
            spriteFundo = new TextureRegion(arquivoTexturaFundo);
        } catch (Exception e) {
            Gdx.app.error("classeprincipal", "ih, nao deu pra carregar alguma imagem. da uma olhada na pasta assets.", e);
        }

        palco = new Stage(new ScreenViewport());
        criarSkinBase();
        
        tabelaUI = new Table();
        tabelaUI.setFillParent(true);
        tabelaUI.top().left().pad(10);
        palco.addActor(tabelaUI);

        TextButton botaoReiniciar = new TextButton("Reiniciar Simulacao", skinUI);
        botaoReiniciar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent evento, Actor ator) {
                reiniciarSimulacao();
            }
        });
        tabelaUI.add(botaoReiniciar).left().colspan(3).padBottom(10).row();

        tabelaUI.add(new Label("Qtd Boids:", skinUI, "default")).left();
        final Slider sliderAgentes = new Slider(0, 2000, 50, false, skinUI);
        sliderAgentes.setValue(contagemAtualAgentes);
        sliderAgentes.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent evento, Actor ator) {
                definirContagemAgentes((int)sliderAgentes.getValue());
            }
        });
        rotuloContagemAgentesUI = new Label("", skinUI);
        tabelaUI.add(sliderAgentes).width(200).padLeft(5).padRight(5);
        tabelaUI.add(rotuloContagemAgentesUI).left().row();

        tabelaUI.add(new Label("Vel Simulacao:", skinUI, "default")).left();
        //voce tinha aumentado o maximo do slider de velocidade para 50f, mantendo isso
        final Slider sliderVelocidade = new Slider(0.1f, 50f, 0.1f, false, skinUI); 
        sliderVelocidade.setValue(velocidadeSimulacao);
        sliderVelocidade.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent evento, Actor ator) {
                definirVelocidadeSimulacao(sliderVelocidade.getValue());
            }
        });
        rotuloVelocidadeSimulacaoUI = new Label("", skinUI);
        tabelaUI.add(sliderVelocidade).width(200).padLeft(5).padRight(5);
        tabelaUI.add(rotuloVelocidadeSimulacaoUI).left().row();

        //novo: slider para o peso de coesao (agrupamento)
        tabelaUI.add(new Label("Peso Coesao:", skinUI, "default")).left();
        //o slider vai de -1.0 (repulsao) ate 1.0 (atracao), com passos de 0.1
        final Slider sliderPesoCoesao = new Slider(-1.0f, 1.0f, 0.1f, false, skinUI);
        sliderPesoCoesao.setValue(Boid.pesoCoesaoGlobal); //define o valor inicial do slider com o valor da classe boid
        sliderPesoCoesao.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent evento, Actor ator) {
                definirPesoCoesao(sliderPesoCoesao.getValue());
            }
        });
        rotuloPesoCoesaoUI = new Label("", skinUI); //label pra mostrar o valor atual do peso
        tabelaUI.add(sliderPesoCoesao).width(200).padLeft(5).padRight(5);
        tabelaUI.add(rotuloPesoCoesaoUI).left().row();


        InputMultiplexer multiplexadorInput = new InputMultiplexer();
        multiplexadorInput.addProcessor(palco);
        multiplexadorInput.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int codigoTecla) {
                if (codigoTecla == Input.Keys.P) { //controle para alternar entre modo sequencial e paralelo. [cite: 3]
                    modoParalelo = !modoParalelo;
                    Gdx.app.log("classeprincipal", "modo alterado para: " + (modoParalelo ? "paralelo" : "sequencial"));
                    atualizarTextoInfo();
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(multiplexadorInput);

        agentesLista = new ArrayList<>();
        gerarAgentes(CONTAGEM_INICIAL_AGENTES); //a simulacao deve ter no minimo 500 agentes. [cite: 2]

        numeroDeThreads = Runtime.getRuntime().availableProcessors();
        atualizarTextoInfo();

        Gdx.app.log("classeprincipal", "simulacao iniciada. ui configurada e tudo pronto!");
    }
    
    private void criarSkinBase() {
        skinUI = new Skin();
        skinUI.add("default-font", fonte, BitmapFont.class);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skinUI.add("white", new Texture(pixmap));
        pixmap.dispose();

        TextButton.TextButtonStyle estiloBotaoTexto = new TextButton.TextButtonStyle();
        estiloBotaoTexto.up = skinUI.newDrawable("white", new Color(0.4f, 0.4f, 0.4f, 1f));
        estiloBotaoTexto.down = skinUI.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1f));
        estiloBotaoTexto.over = skinUI.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f));
        estiloBotaoTexto.font = skinUI.getFont("default-font");
        estiloBotaoTexto.fontColor = Color.WHITE;
        skinUI.add("default", estiloBotaoTexto);

        Label.LabelStyle estiloLabel = new Label.LabelStyle();
        estiloLabel.font = skinUI.getFont("default-font");
        estiloLabel.fontColor = Color.WHITE;
        skinUI.add("default", estiloLabel);

        Slider.SliderStyle estiloSlider = new Slider.SliderStyle();
        estiloSlider.background = skinUI.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 1f));
        
        //mantendo os tamanhos de knob que voce ajustou
        float larguraKnob = 5f; 
        float alturaKnob = 5f; 
        
        Drawable knobNormal = skinUI.newDrawable("white", Color.LIGHT_GRAY);
        knobNormal.setMinWidth(larguraKnob);
        knobNormal.setMinHeight(alturaKnob);
        estiloSlider.knob = knobNormal;

        Drawable knobMouseEmCima = skinUI.newDrawable("white", Color.WHITE);
        knobMouseEmCima.setMinWidth(larguraKnob);
        knobMouseEmCima.setMinHeight(alturaKnob);
        estiloSlider.knobOver = knobMouseEmCima;

        Drawable knobPressionado = skinUI.newDrawable("white", Color.DARK_GRAY);
        knobPressionado.setMinWidth(larguraKnob);
        knobPressionado.setMinHeight(alturaKnob);
        estiloSlider.knobDown = knobPressionado;
        skinUI.add("default-horizontal", estiloSlider);
    }

    public void reiniciarSimulacao() {
        Gdx.app.log("classeprincipal", "opa, recomecando a bagunca...");
        agentesLista.clear();
        gerarAgentes(this.contagemAtualAgentes);
    }

    private void gerarAgentes(int contagemAlvo) {
        Random random = new Random();
        while (agentesLista.size() > contagemAlvo && !agentesLista.isEmpty()) {
            agentesLista.remove(agentesLista.size() - 1);
        }
        //cada agente deve ter uma posicao e direcao. [cite: 2]
        for (int i = agentesLista.size(); i < contagemAlvo; i++) {
            agentesLista.add(new Boid(random.nextFloat() * larguraTela, random.nextFloat() * alturaTela));
        }
        this.contagemAtualAgentes = agentesLista.size();
        atualizarTextoInfo();
    }

    @Override
    public void render() {
        float tempoDelta = Gdx.graphics.getDeltaTime();
        
        palco.act(Math.min(tempoDelta, 1 / 30f));

        long tempoInicioNano = System.nanoTime(); //utilize o system.currenttimemillis() para medir tempo de atualizacao por frame. [cite: 3]
        if (modoParalelo) {
            atualizarParalelo(tempoDelta); //versao paralela: dividir o vetor de agentes em chunks e atualizar cada chunk com uma thread separada. [cite: 2]
        } else {
            atualizarSequencial(tempoDelta); //versao sequencial: atualizacao de todos os agentes em um unico loop. [cite: 2]
        }
        ultimoTempoAtualizacaoMillis = (System.nanoTime() - tempoInicioNano) / 1_000_000f;

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1, true);
        
        loteDeSprites.begin();
        
        //desenhando background que voce adicionou
        if (spriteFundo != null) {
            loteDeSprites.draw(spriteFundo,
                    0, 0,           //posicao x, y
                    larguraTela, alturaTela); //largura e altura para preencher a tela
        }
        
        if (spriteBoid != null) {
            for (Boid agente : agentesLista) { //agentes visiveis (como circulos ou sprites simples). [cite: 3]
                agente.renderizar(loteDeSprites, spriteBoid);
            }
        }
        atualizarTextoInfoDinamicos();
        //ajustando a posicao dos textos de debug para nao sobrepor a ui, talvez precise de mais ajuste
        float alturaUiEstimada = tabelaUI.getPadTop() + tabelaUI.getPrefHeight() + 10; //pega a altura da tabela ui
        fonte.draw(loteDeSprites, textoModoAtual, 10, alturaUiEstimada + 60);
        fonte.draw(loteDeSprites, textoTempoAtualizacao, 10, alturaUiEstimada + 40);
        fonte.draw(loteDeSprites, textoFps, 10, alturaUiEstimada + 20);
        loteDeSprites.end();

        palco.draw();
    }

    private void atualizarSequencial(float tempoDelta) {
        for (Boid agente : agentesLista) {
            //movimentar-se de acordo com regras estipuladas para o algoritmo de boids. [cite: 2]
            agente.calcularForcasDirecionaisECor(agentesLista);
        }
        for (Boid agente : agentesLista) {
            agente.aplicarMovimento(tempoDelta, larguraTela, alturaTela, velocidadeSimulacao);
        }
    }

    private void atualizarParalelo(float tempoDelta) {
        int totalAgentes = agentesLista.size();
        if (totalAgentes == 0) return;

        List<Thread> threads = new ArrayList<>(); //threads: java.lang.thread. [cite: 3]
        int tamanhoBloco = (totalAgentes + numeroDeThreads - 1) / numeroDeThreads;

        for (int i = 0; i < numeroDeThreads; i++) {
            int inicio = i * tamanhoBloco;
            int fim = Math.min(inicio + tamanhoBloco, totalAgentes);
            
            if (inicio < fim) {
                //use estruturas de dados simples (como arraylist). [cite: 3]
                List<Boid> subLista = agentesLista.subList(inicio, fim);
                //a tarefa principal sera paralelizar a atualizacao do estado dos agentes sem causar concorrencia de dados cada thread lidara com um subconjunto isolado dos agentes. [cite: 2]
                Thread t = new Thread(new AgentUpdateTask(subLista, agentesLista, tempoDelta, larguraTela, alturaTela, velocidadeSimulacao));
                threads.add(t);
                t.start();
            }
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Gdx.app.error("classeprincipal", "ops, uma thread de atualizacao foi interrompida!", e);
            }
        }
        //a atualizacao paralela deve evitar qualquer tipo de escrita concorrente. [cite: 2]
        //cada thread atualiza apenas os agentes do seu chunk, e os agentes nao compartilham estado (durante a escrita). [cite: 2]
    }
    
    private void atualizarTextoInfo() {
        textoModoAtual = "modo: " + (modoParalelo ? "paralelo (" + numeroDeThreads + "t)" : "sequencial");
        
        if(rotuloContagemAgentesUI != null) {
            rotuloContagemAgentesUI.setText("Atual: " + contagemAtualAgentes);
        }
        if(rotuloVelocidadeSimulacaoUI != null) {
            rotuloVelocidadeSimulacaoUI.setText(String.format("Atual: %.1fx", velocidadeSimulacao));
        }
        //novo: atualiza o label do peso de coesao
        if(rotuloPesoCoesaoUI != null) {
            rotuloPesoCoesaoUI.setText(String.format("Atual: %.1f", Boid.pesoCoesaoGlobal));
        }
        atualizarTextoInfoDinamicos();
    }

    private void atualizarTextoInfoDinamicos(){
        //contador de fps ou tempo medio de atualizacao por frame. 
        textoTempoAtualizacao = String.format("update: %.2f ms", ultimoTempoAtualizacaoMillis);
        textoFps = "fps: " + Gdx.graphics.getFramesPerSecond();
    }

    @Override
    public void resize(int largura, int altura) {
        larguraTela = largura;
        alturaTela = altura;
        if (loteDeSprites != null) {
            loteDeSprites.getProjectionMatrix().setToOrtho2D(0, 0, largura, altura);
        }
        if (palco != null) {
            palco.getViewport().update(largura, altura, true);
        }
        if (tabelaUI != null) {
            tabelaUI.invalidateHierarchy();
        }
        Gdx.app.log("classeprincipal", "tela esticada para: " + largura + "x" + altura);
    }

    @Override
    public void dispose() {
        if (loteDeSprites != null) loteDeSprites.dispose();
        if (fonte != null) fonte.dispose();
        if (arquivoTexturaBoid != null) arquivoTexturaBoid.dispose();
        if (arquivoTexturaFundo != null) arquivoTexturaFundo.dispose(); //nao esquecer de liberar o fundo
        if (palco != null) palco.dispose();
        if (skinUI != null) skinUI.dispose();
        Gdx.app.log("classeprincipal", "simulacao finalizada, tudo limpinho agora!");
    }

    public void definirContagemAgentes(int contagem) {
        int novaContagem = Math.max(0, contagem);
        novaContagem = Math.min(novaContagem, 2000);
        
        if (novaContagem != this.contagemAtualAgentes) {
            gerarAgentes(novaContagem);
        } else {
             atualizarTextoInfo();
        }
    }

    public int obterContagemAgentes() {
        return this.contagemAtualAgentes;
    }

    public void definirVelocidadeSimulacao(float velocidade) {
        this.velocidadeSimulacao = Math.max(0.1f, velocidade);
        //voce tinha alterado o maximo do slider para 50f, entao o clamp aqui tambem deve ser 50f
        this.velocidadeSimulacao = Math.min(this.velocidadeSimulacao, 50f); 
        atualizarTextoInfo();
    }

    public float obterVelocidadeSimulacao() {
        return this.velocidadeSimulacao;
    }

    //novo: metodo para definir o peso da coesao, chamado pelo listener do slider
    public void definirPesoCoesao(float novoPeso) {
        //os limites do slider sao -1.0 e 1.0, entao vamos garantir que o valor fique nesse intervalo
        Boid.pesoCoesaoGlobal = Math.max(-1.0f, Math.min(novoPeso, 1.0f));
        atualizarTextoInfo(); //atualiza o label da ui
        Gdx.app.log("classeprincipal", "peso de coesao global definido para: " + Boid.pesoCoesaoGlobal);
    }
}