//arquivo: Boid.java
package io.github.boids;

//importacoes do libgdx que a gente vai precisar pra essa classe
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import java.util.List;
import java.util.Random;

public class Boid {
    //onde o boid esta no mundo? x e y guardados aqui
    public Vector2 posicao;
    //pra onde e com que rapidez o boid esta se movendo?
    public Vector2 velocidade;
    //qualquer forca que esta agindo no boid (tipo empurrando ele) e acumulada aqui
    public Vector2 aceleracao;

    //ate que distancia o boid consegue "ver" outros pra se alinhar e ficar junto?
    float raioPercepcao;
    //qual a distancia minima que o boid tenta manter dos outros pra nao bater?
    float distanciaSeparacao;
    //qual a "forca" maxima que o boid pode usar pra mudar de direcao?
    float forcaMaxima;
    //e qual a velocidade maxima que ele pode atingir?
    float velocidadeMaxima;

    //raio pra checar quantos vizinhos estao bem pertinho pra mudar a cor
    private float raioDensidade;
    //numero maximo de vizinhos pra cor ficar totalmente vermelha (tipo, ta lotado!)
    private int maxVizinhosParaDensidade;
    //a cor do boid, que vai mudar dependendo de quantos vizinhos ele tem por perto
    public Color corAgrupamento;

    //novo: variavel estatica para o peso da coesao, controlada pela ui
    //comeca com 1.0f (comportamento padrao de atracao)
    public static float pesoCoesaoGlobal = 1.0f;

    private static Random aleatorio = new Random();

    public Boid(float x, float y) {
        posicao = new Vector2(x, y);
        velocidade = new Vector2(aleatorio.nextFloat() * 2 - 1, aleatorio.nextFloat() * 2 - 1);
        velocidade.nor();
        velocidade.scl(aleatorio.nextFloat() * 2 + 2);
        aceleracao = new Vector2();

        raioPercepcao = 50f;
        distanciaSeparacao = 25f;
        forcaMaxima = 0.2f;
        velocidadeMaxima = 4f;

        raioDensidade = 30f;
        maxVizinhosParaDensidade = 10;
        corAgrupamento = new Color(Color.RED); //cor inicial, vai mudar
    }

    public void aplicarForca(Vector2 forca) {
        aceleracao.add(forca);
    }

    private Vector2 separar(List<Boid> todosAgentes) {
        Vector2 vetorDirecao = new Vector2();
        int contador = 0;
        for (Boid outroAgente : todosAgentes) {
            if (outroAgente == this) continue;
            float dist = posicao.dst(outroAgente.posicao);
            if ((dist > 0) && (dist < distanciaSeparacao)) {
                Vector2 diferenca = posicao.cpy().sub(outroAgente.posicao);
                diferenca.nor();
                diferenca.scl(1f / dist);
                vetorDirecao.add(diferenca);
                contador++;
            }
        }
        if (contador > 0) {
            vetorDirecao.scl(1f / contador);
        }
        if (vetorDirecao.len() > 0) {
            vetorDirecao.nor();
            vetorDirecao.scl(velocidadeMaxima);
            vetorDirecao.sub(velocidade);
            vetorDirecao.limit(forcaMaxima);
        }
        return vetorDirecao;
    }

    private Vector2 alinhar(List<Boid> todosAgentes) {
        Vector2 somaDasVelocidades = new Vector2();
        int contador = 0;
        for (Boid outroAgente : todosAgentes) {
            if (outroAgente == this) continue;
            float dist = posicao.dst(outroAgente.posicao);
            if ((dist > 0) && (dist < raioPercepcao)) {
                somaDasVelocidades.add(outroAgente.velocidade);
                contador++;
            }
        }
        if (contador > 0) {
            somaDasVelocidades.scl(1f / contador);
            somaDasVelocidades.nor();
            somaDasVelocidades.scl(velocidadeMaxima);
            Vector2 vetorDirecao = somaDasVelocidades.sub(velocidade);
            vetorDirecao.limit(forcaMaxima);
            return vetorDirecao;
        } else {
            return new Vector2();
        }
    }

    private Vector2 coerir(List<Boid> todosAgentes) {
        Vector2 somaDasPosicoes = new Vector2();
        int contador = 0;
        for (Boid outroAgente : todosAgentes) {
            if (outroAgente == this) continue;
            float dist = posicao.dst(outroAgente.posicao);
            if ((dist > 0) && (dist < raioPercepcao)) {
                somaDasPosicoes.add(outroAgente.posicao);
                contador++;
            }
        }
        if (contador > 0) {
            somaDasPosicoes.scl(1f / contador);
            return buscarAlvo(somaDasPosicoes);
        } else {
            return new Vector2();
        }
    }

    private Vector2 buscarAlvo(Vector2 alvo) {
        Vector2 direcaoDesejada = alvo.cpy().sub(posicao);
        direcaoDesejada.nor();
        direcaoDesejada.scl(velocidadeMaxima);
        Vector2 vetorDirecao = direcaoDesejada.sub(velocidade);
        vetorDirecao.limit(forcaMaxima);
        return vetorDirecao;
    }

    public void atualizarCorAgrupamento(List<Boid> todosAgentes) {
        int vizinhosNoRaioDensidade = 0;
        for (Boid outroAgente : todosAgentes) {
            if (outroAgente == this) continue;
            float dist = posicao.dst(outroAgente.posicao);
            if (dist < raioDensidade) {
                vizinhosNoRaioDensidade++;
            }
        }
        float fatorInterpolacao = Math.min((float)vizinhosNoRaioDensidade / maxVizinhosParaDensidade, 1.0f);
        corAgrupamento.set(Color.GREEN).lerp(Color.RED, fatorInterpolacao);
    }

    public void calcularForcasDirecionaisECor(List<Boid> todosAgentes) {
        Vector2 forcaSeparacao = separar(todosAgentes);
        Vector2 forcaAlinhamento = alinhar(todosAgentes);
        Vector2 forcaCoesao = coerir(todosAgentes);

        forcaSeparacao.scl(1.5f);
        forcaAlinhamento.scl(1.0f);
        //aqui esta a mudanca: usamos o pesoCoesaoGlobal!
        //se for positivo, eles se atraem. se for negativo, eles se repelem (anti-coesao).
        //se for zero, a coesao e ignorada.
        forcaCoesao.scl(pesoCoesaoGlobal); //antes era forcaCoesao.scl(1.0f);

        aplicarForca(forcaSeparacao);
        aplicarForca(forcaAlinhamento);
        aplicarForca(forcaCoesao);

        atualizarCorAgrupamento(todosAgentes);
    }

    public void aplicarMovimento(float tempoDelta, float larguraTela, float alturaTela, float velocidadeSimulacao) {
        float tempoDeltaEfetivo = tempoDelta * velocidadeSimulacao;
        velocidade.add(aceleracao.cpy().scl(tempoDeltaEfetivo));
        velocidade.limit(velocidadeMaxima);
        posicao.add(velocidade.cpy().scl(tempoDeltaEfetivo));
        aceleracao.set(0, 0);
        verificarBordas(larguraTela, alturaTela);
    }

    private void verificarBordas(float larguraTela, float alturaTela) {
        if (posicao.x > larguraTela) posicao.x = 0;
        else if (posicao.x < 0) posicao.x = larguraTela;
        if (posicao.y > alturaTela) posicao.y = 0;
        else if (posicao.y < 0) posicao.y = alturaTela;
    }

    public void renderizar(SpriteBatch loteSprites, TextureRegion texturaSprite) {
        if (texturaSprite == null) return;
        float angulo = velocidade.angleDeg();
        float larguraSprite = texturaSprite.getRegionWidth() / 100f;
        float alturaSprite = texturaSprite.getRegionHeight() / 100f;
        float origemX = larguraSprite / 2f;
        float origemY = alturaSprite / 2f;

        loteSprites.setColor(this.corAgrupamento);
        loteSprites.draw(texturaSprite,
                   posicao.x - origemX, posicao.y - origemY,
                   origemX, origemY,
                   larguraSprite, alturaSprite,
                   1f, 1f,
                   angulo);
        loteSprites.setColor(Color.WHITE);
    }
}