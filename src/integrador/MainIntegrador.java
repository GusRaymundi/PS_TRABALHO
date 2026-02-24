package integrador;

import executor.*;
import montador.*;
import macro.MainMacro;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

// Integrador - junta macro, montador, ligador, carregador e simulador
public class MainIntegrador extends JFrame {

    private JTextArea areaLog;
    private JTextField campoArquivo;
    private JCheckBox checkMacro;
    private JCheckBox checkMultiplosModulos;
    private JTextField campoEndCarga;
    private InterfaceSimulador simulador;

    public MainIntegrador() {
        super("SIC/XE - Sistema Integrado");
        inicializarUI();
    }

    private void inicializarUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // config
        JPanel painelConfig = new JPanel(new GridBagLayout());
        painelConfig.setBorder(BorderFactory.createTitledBorder("Configuracao"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        painelConfig.add(new JLabel("Arquivo fonte (.asm):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        campoArquivo = new JTextField("programa.asm", 30);
        painelConfig.add(campoArquivo, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton btnEscolher = new JButton("...");
        btnEscolher.addActionListener(e -> escolherArquivo());
        painelConfig.add(btnEscolher, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        checkMacro = new JCheckBox("Processar macros antes da montagem");
        painelConfig.add(checkMacro, gbc);

        gbc.gridy = 2;
        checkMultiplosModulos = new JCheckBox("Ligar multiplos modulos (pasta objetos/)");
        painelConfig.add(checkMultiplosModulos, gbc);

        gbc.gridy = 3; gbc.gridwidth = 1;
        painelConfig.add(new JLabel("Endereco de carga (hex):"), gbc);
        gbc.gridx = 1;
        campoEndCarga = new JTextField("0000", 10);
        painelConfig.add(campoEndCarga, gbc);

        add(painelConfig, BorderLayout.NORTH);

        // Area de log central
        areaLog = new JTextArea(20, 60);
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(areaLog), BorderLayout.CENTER);

        // Botoes inferiores
        JPanel painelBotoes = new JPanel(new FlowLayout());

        JButton btnMontar = new JButton("1. Montar");
        btnMontar.addActionListener(this::executarMontagem);
        painelBotoes.add(btnMontar);

        JButton btnLigar = new JButton("2. Ligar");
        btnLigar.addActionListener(this::executarLigacao);
        painelBotoes.add(btnLigar);

        JButton btnCarregarExecutar = new JButton("3. Carregar e Executar");
        btnCarregarExecutar.addActionListener(this::executarCarregamentoEExecucao);
        painelBotoes.add(btnCarregarExecutar);

        JButton btnTudo = new JButton("Executar Tudo");
        btnTudo.setForeground(new Color(0, 100, 0));
        btnTudo.addActionListener(this::executarTudo);
        painelBotoes.add(btnTudo);

        add(painelBotoes, BorderLayout.SOUTH);

        setSize(750, 550);
        setLocationRelativeTo(null);
    }

    private void escolherArquivo() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setDialogTitle("Selecionar arquivo .asm");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoArquivo.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            areaLog.append(msg + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    private void executarMontagem(ActionEvent e) {
        areaLog.setText("");
        new Thread(() -> {
            try {
                String arquivo = campoArquivo.getText().trim();

                // Etapa 1: Processamento de macros (se ativado)
                if (checkMacro.isSelected()) {
                    log("=== ETAPA 1: PROCESSAMENTO DE MACROS ===");
                    log("Processando macros de: " + arquivo);
                    MainMacro.main(new String[]{arquivo});
                    arquivo = "MASMAPRG.ASM";
                    log("Macros expandidas para: MASMAPRG.ASM");
                    log("");
                }

                // Etapa 2: Montagem
                log("=== ETAPA 2: MONTAGEM (duas passagens) ===");
                log("Montando: " + arquivo);

                List<String> linhasFonte = Files.readAllLines(Paths.get(arquivo));
                Montador montador = new Montador(linhasFonte);

                log("Primeira passagem...");
                montador.primeiraPassagem();

                log("Tabela de simbolos:");
                for (Map.Entry<String, Integer> entry : montador.getTabelaSimbolos().getMapaSimbolos().entrySet()) {
                    log(String.format("   %-12s = 0x%04X", entry.getKey(), entry.getValue()));
                }

                log("Segunda passagem...");
                List<String> codigoHex = montador.segundaPassagem();

                Files.write(Paths.get("programa.txt"), codigoHex);
                String nomeBase = Paths.get(arquivo).getFileName().toString().replace(".asm", "").replace(".ASM", "");
                montador.salvarArquivoObjeto(nomeBase + ".obj", codigoHex);

                log("Codigo objeto gerado (" + codigoHex.size() + " instrucoes):");
                for (int i = 0; i < codigoHex.size(); i++) {
                    log(String.format("   %04X: %s", i * 3, codigoHex.get(i)));
                }

                log("");
                log("programa.txt gerado (formato simulador)");
                log("objetos/" + nomeBase + ".obj gerado (formato ligador)");
                log("Montagem concluida com sucesso!");

            } catch (Exception ex) {
                log("ERRO na montagem: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void executarLigacao(ActionEvent e) {
        new Thread(() -> {
            try {
                log("");
                log("=== ETAPA 3: LIGACAO ===");

                File pasta = new File("objetos");
                if (!pasta.exists() || pasta.listFiles() == null) {
                    log("ERRO: Pasta 'objetos' nao encontrada. Monte o programa primeiro.");
                    return;
                }

                File[] arquivos = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".obj"));
                if (arquivos == null || arquivos.length == 0) {
                    log("ERRO: Nenhum arquivo .obj encontrado.");
                    return;
                }

                List<String> arquivosObj = new ArrayList<>();
                for (File f : arquivos) {
                    arquivosObj.add(f.getAbsolutePath());
                }
                Collections.sort(arquivosObj);

                log("Modulos encontrados: " + arquivosObj.size());
                for (String a : arquivosObj) {
                    log("   " + new File(a).getName());
                }

                int endCarga;
                try {
                    endCarga = Integer.parseInt(campoEndCarga.getText().trim(), 16);
                } catch (NumberFormatException ex) {
                    endCarga = 0;
                }

                boolean modoSimples = (arquivosObj.size() == 1 && endCarga == 0);
                int enderecoCarga = modoSimples ? -1 : endCarga;

                Ligador ligador = new Ligador(enderecoCarga);

                log("Passagem 1: Mapeamento de modulos...");
                ligador.primeiraPassagem(arquivosObj);
                ligador.exibirTabelaGlobal();

                log("Passagem 2: Relocacao e resolucao...");
                List<String> codigoFinal = ligador.segundaPassagem();

                ligador.salvarArquivoFinal(codigoFinal, "executavel.obj");

                log("Modo: " + (modoSimples ? "LIGADOR SIMPLES" : "LIGADOR-RELOCADOR"));
                log("Arquivo executavel.obj gerado!");
                log("Ligacao concluida com sucesso!");

            } catch (Exception ex) {
                log("ERRO na ligacao: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void executarCarregamentoEExecucao(ActionEvent e) {
        new Thread(() -> {
            try {
                log("");
                log("=== ETAPA 4: CARREGAMENTO E EXECUCAO ===");

                String caminhoExecutavel = "executavel.obj";
                Path execPath = Paths.get(caminhoExecutavel);
                Path progPath = Paths.get("programa.txt");

                if (!Files.exists(execPath) && !Files.exists(progPath)) {
                    log("ERRO: Nenhum programa encontrado. Monte e ligue primeiro.");
                    return;
                }

                boolean usarExecutavel = Files.exists(execPath);
                log("Abrindo simulador grafico...");

                SwingUtilities.invokeLater(() -> {
                    try {
                        simulador = new InterfaceSimulador();
                        simulador.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        simulador.setVisible(true);

                        Maquina maq = simulador.getMaquina();
                        Carregador carregador = new Carregador(maq);

                        if (usarExecutavel) {
                            log("Carregando executavel.obj via Carregador...");

                            int deslocamento = 0;
                            if (carregador.isModoSimples(caminhoExecutavel)) {
                                log("Modo: CARREGADOR RELOCADOR");
                                String entrada = JOptionPane.showInputDialog(simulador,
                                        "MODO SIMPLES: Endereco de carga (Hex):", "0000");
                                if (entrada != null) {
                                    deslocamento = Integer.parseInt(entrada.trim(), 16);
                                }
                            } else {
                                log("Modo: CARREGADOR ABSOLUTO");
                            }

                            int enderecoInicial = carregador.carregar(caminhoExecutavel, deslocamento);
                            if (enderecoInicial >= 0) {
                                maq.getCpu().PC().setValor(enderecoInicial);
                            }
                            log(String.format("Programa carregado na memoria (PC = %04X)", enderecoInicial));
                        } else {
                            log("Carregando programa.txt diretamente...");
                            List<String> linhas = Files.readAllLines(progPath);
                            List<String> programa = new ArrayList<>();
                            for (String l : linhas) {
                                l = l.trim();
                                if (!l.isEmpty()) programa.add(l);
                            }
                            maq.carregarProgramaHex(programa, 0x0000);
                            log("Programa carregado na memoria (PC = 0000)");
                        }

                        log("Simulador aberto!");
                        log("Use os botoes Passo/Executar tudo para executar o programa.");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MainIntegrador.this,
                                "Erro ao abrir simulador: " + ex.getMessage());
                    }
                });

            } catch (Exception ex) {
                log("ERRO: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void executarTudo(ActionEvent e) {
        executarMontagem(e);
        // espera a montagem terminar antes de seguir
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                executarLigacao(e);
                Thread.sleep(1000);
                executarCarregamentoEExecucao(e);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainIntegrador integrador = new MainIntegrador();
            integrador.setVisible(true);
        });
    }
}
