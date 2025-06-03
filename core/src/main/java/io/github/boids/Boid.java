package io.github.boids;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2; 
import java.util.List;
import java.util.Random;

public class Boid {
  //posicao: localizacao atual do boid
  public Vector2 position;
  //velocidade: direcao e magnitude do movimento do boid
  public Vector2 velocity;
  //aceleracao: usada para acumular forças em cada passo
  public Vector2 acceleration;

  //raioparapercepcao: distancia que um boid pode "ver" outros boids para alinhamento e coesao
  float perceptionRadius;
  //distanciaseparacao: distancia minima que um boid tenta manter de outros boids
  float separationDistance;
  //forcamaxima: limita a magnitude do vetor de direcao (steering)
  float maxForce;
  //velocidademaxima: limita a magnitude da velocidade
  float maxSpeed;

  private static Random random = new Random();

  //construtor: inicializa um boid em uma posicao aleatoria
  public Boid(float x, float y) {
      position = new Vector2(x, y);
      velocity = new Vector2(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1);
      velocity.nor();
      velocity.scl(random.nextFloat() * 2 + 2);
      acceleration = new Vector2();

      perceptionRadius = 50f;
      separationDistance = 25f;
      maxForce = 0.2f;
      maxSpeed = 4f;
  }

  //aplicarforca: adiciona uma forca ao vetor de aceleracao
  public void applyForce(Vector2 force) {
      acceleration.add(force);
  }

  //separacao: os agentes evitam colisoes com seus vizinhos. [cite: 5]
  private Vector2 separate(List<Boid> boids) {
      Vector2 steer = new Vector2();
      int count = 0;
      for (Boid other : boids) {
          if (other == this) continue;
          float d = position.dst(other.position);
          if ((d > 0) && (d < separationDistance)) {
              Vector2 diff = position.cpy().sub(other.position);
              diff.nor();
              diff.scl(1f / d);
              steer.add(diff);
              count++;
          }
      }
      if (count > 0) {
          steer.scl(1f / count);
      }
      if (steer.len() > 0) {
          steer.nor();
          steer.scl(maxSpeed);
          steer.sub(velocity);
          steer.limit(maxForce);
      }
      return steer;
  }

  //alinhamento: os agentes tentam mover-se na mesma direcao que os seus vizinhos. [cite: 6]
  private Vector2 align(List<Boid> boids) {
      Vector2 sum = new Vector2();
      int count = 0;
      for (Boid other : boids) {
          if (other == this) continue;
          float d = position.dst(other.position);
          if ((d > 0) && (d < perceptionRadius)) {
              sum.add(other.velocity);
              count++;
          }
      }
      if (count > 0) {
          sum.scl(1f / count);
          sum.nor();
          sum.scl(maxSpeed);
          Vector2 steer = sum.sub(velocity);
          steer.limit(maxForce);
          return steer;
      } else {
          return new Vector2();
      }
  }

  //coesao: os agentes tendem a ficar proximos uns dos outros, movendo-se em direcao ao centro da multidao local. [cite: 7]
  private Vector2 cohesion(List<Boid> boids) {
      Vector2 sum = new Vector2();
      int count = 0;
      for (Boid other : boids) {
          if (other == this) continue;
          float d = position.dst(other.position);
          if ((d > 0) && (d < perceptionRadius)) {
              sum.add(other.position);
              count++;
          }
      }
      if (count > 0) {
          sum.scl(1f / count);
          return seek(sum);
      } else {
          return new Vector2();
      }
  }

  //seek: calcula uma forca de direcao para um alvo desejado
  private Vector2 seek(Vector2 target) {
      Vector2 desired = target.cpy().sub(position);
      desired.nor();
      desired.scl(maxSpeed);
      Vector2 steer = desired.sub(velocity);
      steer.limit(maxForce);
      return steer;
  }

  //calculatesteeringforces: aplica todas as tres regras de boids
  //movimentar-se de acordo com regras estipuladas para o algoritmo de boids. [cite: 14]
  public void calculateSteeringForces(List<Boid> boids) {
      Vector2 sep = separate(boids);
      Vector2 ali = align(boids);
      Vector2 coh = cohesion(boids);

      sep.scl(1.5f); //peso para separacao
      ali.scl(1.0f); //peso para alinhamento
      coh.scl(1.0f); //peso para coesao

      applyForce(sep);
      applyForce(ali);
      applyForce(coh);
  }

  //applymotion: atualiza o estado do boid
  public void applyMotion(float deltaTime, float screenWidth, float screenHeight) {
      velocity.add(acceleration.scl(deltaTime));
      velocity.limit(maxSpeed);
      position.add(velocity.cpy().scl(deltaTime));
      acceleration.set(0, 0);
      edges(screenWidth, screenHeight);
  }

  //edges: faz os boids darem a volta na tela
  private void edges(float screenWidth, float screenHeight) {
      if (position.x > screenWidth) position.x = 0;
      else if (position.x < 0) position.x = screenWidth;
      if (position.y > screenHeight) position.y = 0;
      else if (position.y < 0) position.y = screenHeight;
  }

  //render: desenha o boid na tela
  //agentes visiveis (como circulos ou sprites simples). [cite: 20]
  public void render(ShapeRenderer renderer) {
      float angle = velocity.angleDeg();
      float size = 5.0f;

      renderer.translate(position.x, position.y, 0);
      renderer.rotate(0, 0, 1, angle);
      renderer.triangle(size, 0, -size, -size / 2, -size, size / 2);
      renderer.rotate(0, 0, 1, -angle);
      renderer.translate(-position.x, -position.y, 0);
  }
}