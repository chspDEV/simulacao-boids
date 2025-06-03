package io.github.boids;

import java.util.List;

//a tarefa principal sera paralelizar a atualizacao do estado dos agentes sem causar concorrencia de dados cada thread lidara com um subconjunto isolado dos agentes. [cite: 12]
public class AgentUpdateTask implements Runnable {
  private List<Boid> allBoids;
  private List<Boid> boidsToUpdate; //subconjunto de agentes que esta thread ira atualizar
  private float deltaTime;
  private float screenWidth;
  private float screenHeight;

  public AgentUpdateTask(List<Boid> boidsToUpdate, List<Boid> allBoids, float deltaTime, float screenWidth, float screenHeight) {
      this.boidsToUpdate = boidsToUpdate;
      this.allBoids = allBoids;
      this.deltaTime = deltaTime;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
  }

  @Override
  public void run() {
      //cada thread atualiza apenas os agentes do seu chunk. [cite: 18]
      //e os agentes nao compartilham estado (durante a escrita de suas proprias propriedades). [cite: 18]
      //a atualizacao paralela deve evitar qualquer tipo de escrita concorrente. [cite: 17]
      for (Boid boid : boidsToUpdate) {
          boid.calculateSteeringForces(allBoids); //leitura de estados de 'allboids'
      }
      for (Boid boid : boidsToUpdate) {
          boid.applyMotion(deltaTime, screenWidth, screenHeight); //escrita apenas no 'boid' pertencente a 'boidstoupdate'
      }
  }
}