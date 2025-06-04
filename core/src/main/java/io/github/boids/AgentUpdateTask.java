//arquivo: AgentUpdateTask.java
package io.github.boids; //o pacote a gente mantem como voce definiu, ok?

//vamos precisar da nossa lista de boids (agentes) aqui
import java.util.List;

//essa classe e tipo um "trabalhador" que cada thread vai usar pra atualizar um pedaco dos nossos agentes
//ela implementa runnable, que e o jeito do java de dizer "ei, isso aqui pode rodar numa thread separada!"
public class AgentUpdateTask implements Runnable { //nome da classe mantido como agentupdatetask
    //a lista com todos os agentes da simulacao, pra gente poder ler o estado deles
    private List<Boid> listaCompletaAgentes;
    //aqui e o pedacinho (sublista) de agentes que essa tarefa especifica vai cuidar de atualizar
    private List<Boid> agentesParaAtualizar;
    //o tempo que passou desde o ultimo frame, pra manter o movimento suave
    private float tempoDelta;
    //largura e altura da tela, pros agentes saberem onde sao as bordas
    private float larguraTela;
    private float alturaTela;
    //a velocidade da simulacao, pra gente poder acelerar ou deixar em camera lenta
    private float velocidadeSimulacao;

    //esse e o construtor da nossa tarefa, ele recebe tudo que precisa pra trabalhar
    public AgentUpdateTask(List<Boid> agentesParaAtualizar, List<Boid> listaCompletaAgentes,
                           float tempoDelta, float larguraTela, float alturaTela,
                           float velocidadeSimulacao) { //nome do construtor mantido como agentupdatetask
        //aqui a gente so guarda os valores que foram passados nas nossas variaveis internas
        this.agentesParaAtualizar = agentesParaAtualizar;
        this.listaCompletaAgentes = listaCompletaAgentes;
        this.tempoDelta = tempoDelta;
        this.larguraTela = larguraTela;
        this.alturaTela = alturaTela;
        this.velocidadeSimulacao = velocidadeSimulacao;
    }

    //o metodo e onde a magica da thread acontece
    //quando a thread comeca, ela roda o que estiver aqui dentro
    @Override
    public void run() {
        //primeiro, a gente passa por todos os agentes que essa thread e responsavel...
        for (Boid agente : agentesParaAtualizar) {
            //...e manda cada um calcular suas forcas e atualizar sua cor
            //ele vai olhar pra listacompletaagentes pra ver os vizinhos, mas so vai mudar a si mesmo
            agente.calcularForcasDirecionaisECor(listaCompletaAgentes);
        }
        //depois que todos calcularam suas proximas acoes e cores...
        for (Boid agente : agentesParaAtualizar) {
            //...a gente manda cada um aplicar o movimento de fato
            //passando o tempodelta, as dimensoes da tela e a velocidadedasimulacao
            agente.aplicarMovimento(tempoDelta, larguraTela, alturaTela, velocidadeSimulacao);
        }
        //e e isso! o trabalho dessa thread pra esse frame acabou. simples, ne?
    }
}