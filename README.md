# Simulador SIC/XE

Implementacao completa de um sistema de software para a maquina hipotetica SIC/XE, baseado no livro "System Software" de Leland L. Beck. O projeto inclui:

- **Montador** (Assembler) de dois passos
- **Processador de Macros** com suporte a macros aninhadas
- **Ligador** com relocacao
- **Carregador** absoluto e relocavel
- **Simulador/Executor** com interface grafica (Swing)

## Requisitos

- Java JDK 8 ou superior

## Compilacao

```bash
javac -d bin src/executor/*.java src/montador/*.java src/macro/*.java
```

## Execucao

```bash
java -cp bin executor.InterfaceSimulador
```

Abre a interface grafica do simulador SIC/XE.

## Estrutura do Projeto

```
src/
  executor/      - Simulador da CPU SIC/XE com interface grafica
  montador/      - Montador de dois passos e Ligador
  macro/         - Processador de macros
```

## Integrantes

| [<img loading="lazy" src="https://avatars.githubusercontent.com/u/90064179?v=4" width=115><br>Henrique Castro](https://github.com/henriquewcastro) | [<img loading="lazy" src="https://avatars.githubusercontent.com/u/125208855?v=4" width=115><br>Marina Brum](https://github.com/marinasbrum) | [<img loading="lazy" src="https://avatars.githubusercontent.com/u/90991800?v=4" width=115><br>Fernando Penedo](https://github.com/penedo97) | [<img loading="lazy" src="https://avatars.githubusercontent.com/u/166431916?v=4" width=115><br>Gustavo Raymundi](https://github.com/GusRaymundi) | [<img loading="lazy" src="https://avatars.githubusercontent.com/u/110736699?v=4" width=115><br>Octavio Ladeira](https://github.com/octalad) | [<img loading="lazy" src="https://avatars.githubusercontent.com/u/120609949?v=4" width=115><br>Wellington Gomes](https://github.com/blckwell) |
| :------------------------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------------------: |
