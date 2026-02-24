package executor;

import montador.Montador;
import montador.Ligador;
import macro.MainMacro;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InterfaceSimulador extends JFrame {

    private Maquina maquina;
    private Path caminhoPrograma;
    private List<String> codigoHexCarregado;
    private boolean programaFinalizado = false;
    private boolean executandoTudo = false;
    private int passoContador = 0;

    // regs
    private final Map<String, JLabel> labelsRegs = new LinkedHashMap<>();

    // tabela mem
    private MemoriaTableModel modeloMemoria;
    private JTable tabelaMemoria;

    // Area de saida/log
    private JTextArea areaSaida;

    // Barra de status
    private JLabel labelStatus;

    // Botoes
    private JButton btnPasso;
    private JButton btnExecutarTudo;
    private JButton btnResetar;


    private static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 14);
    private static final Font FONT_MONO_BOLD = new Font("Monospaced", Font.BOLD, 14);
    private static final Font FONT_REG_VALOR = new Font("Monospaced", Font.BOLD, 18);
    private static final Font FONT_REG_NOME = new Font("SansSerif", Font.BOLD, 12);
    private static final Color COR_FUNDO = new Color(245, 245, 245);
    private static final Color COR_PAINEL = new Color(255, 255, 255);
    private static final Color COR_DESTAQUE = new Color(33, 150, 243);
    private static final Color COR_DESTAQUE_ESCURO = new Color(25, 118, 210);
    private static final Color COR_PC_HIGHLIGHT = new Color(255, 255, 200);

    public InterfaceSimulador() {
        super("Simulador SIC/XE");
        this.maquina = new Maquina(4096);
        construirInterface();
        atualizarTudo();
    }

    public InterfaceSimulador(Maquina maquina, Path caminhoPrograma, List<String> programaOriginal) {
        super("Simulador SIC/XE");
        this.maquina = maquina;
        this.caminhoPrograma = caminhoPrograma;
        this.codigoHexCarregado = programaOriginal;
        this.maquina.carregarProgramaHex(programaOriginal, 0x0000);
        construirInterface();
        atualizarTudo();
        log("Programa carregado: " + caminhoPrograma.getFileName());
    }

    private void construirInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(COR_FUNDO);

        // registradores em cima
        JPanel painelRegistradores = criarPainelRegistradores();
        add(painelRegistradores, BorderLayout.NORTH);

        // memoria e saida no centro
        JSplitPane splitCentro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitCentro.setResizeWeight(0.65);
        splitCentro.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        splitCentro.setBackground(COR_FUNDO);

        JPanel painelMemoria = criarPainelMemoria();
        JPanel painelSaida = criarPainelSaida();

        splitCentro.setLeftComponent(painelMemoria);
        splitCentro.setRightComponent(painelSaida);
        add(splitCentro, BorderLayout.CENTER);

        // botoes embaixo
        JPanel painelInferior = criarPainelInferior();
        add(painelInferior, BorderLayout.SOUTH);

        setSize(1050, 700);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
    }

    // --- registradores ---

    private JPanel criarPainelRegistradores() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(COR_FUNDO);
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JPanel grid = new JPanel(new GridLayout(1, 8, 6, 0));
        grid.setBackground(COR_FUNDO);

        String[] nomes = {"A", "X", "L", "B", "S", "T", "PC", "SW"};
        for (String nome : nomes) {
            JPanel card = criarCardRegistrador(nome);
            grid.add(card);
        }

        container.add(grid, BorderLayout.CENTER);
        return container;
    }

    private JPanel criarCardRegistrador(String nome) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COR_PAINEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JLabel lblNome = new JLabel(nome);
        lblNome.setFont(FONT_REG_NOME);
        lblNome.setForeground(COR_DESTAQUE);
        lblNome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblValor = new JLabel("000000");
        lblValor.setFont(FONT_REG_VALOR);
        lblValor.setForeground(new Color(50, 50, 50));
        lblValor.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblNome);
        card.add(Box.createVerticalStrut(2));
        card.add(lblValor);

        labelsRegs.put(nome, lblValor);
        return card;
    }

    // --- memoria ---

    private JPanel criarPainelMemoria() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(COR_PAINEL);
        painel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Memoria ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), COR_DESTAQUE
        ));

        modeloMemoria = new MemoriaTableModel(maquina.getMemoria());
        tabelaMemoria = new JTable(modeloMemoria);
        tabelaMemoria.setFont(FONT_MONO);
        tabelaMemoria.setRowHeight(22);
        tabelaMemoria.setShowGrid(false);
        tabelaMemoria.setIntercellSpacing(new Dimension(0, 0));
        tabelaMemoria.getTableHeader().setFont(FONT_MONO_BOLD);
        tabelaMemoria.getTableHeader().setReorderingAllowed(false);

        // Coluna de endereco com largura fixa
        tabelaMemoria.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabelaMemoria.getColumnModel().getColumn(0).setMaxWidth(100);

        // Renderer para destacar a linha do PC
        DefaultTableCellRenderer rendererEndereco = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setFont(FONT_MONO_BOLD);
                setForeground(COR_DESTAQUE_ESCURO);
                int pcRow = maquina.getCpu().PC().getValorUnsigned() / 16;
                if (row == pcRow && !isSelected) {
                    c.setBackground(COR_PC_HIGHLIGHT);
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        };
        tabelaMemoria.getColumnModel().getColumn(0).setCellRenderer(rendererEndereco);

        DefaultTableCellRenderer rendererBytes = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setFont(FONT_MONO);
                setForeground(new Color(30, 30, 30));
                int pcRow = maquina.getCpu().PC().getValorUnsigned() / 16;
                if (row == pcRow && !isSelected) {
                    c.setBackground(COR_PC_HIGHLIGHT);
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        };
        tabelaMemoria.getColumnModel().getColumn(1).setCellRenderer(rendererBytes);

        JScrollPane scroll = new JScrollPane(tabelaMemoria);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        painel.add(scroll, BorderLayout.CENTER);

        return painel;
    }

    // --- saida ---

    private JPanel criarPainelSaida() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(COR_PAINEL);
        painel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Saida ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), COR_DESTAQUE
        ));

        areaSaida = new JTextArea();
        areaSaida.setFont(FONT_MONO);
        areaSaida.setEditable(false);
        areaSaida.setBackground(new Color(30, 30, 30));
        areaSaida.setForeground(new Color(200, 255, 200));
        areaSaida.setCaretColor(new Color(200, 255, 200));
        areaSaida.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(areaSaida);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        painel.add(scroll, BorderLayout.CENTER);

        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnLimpar.addActionListener(e -> areaSaida.setText(""));
        JPanel barraLimpar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        barraLimpar.setBackground(COR_PAINEL);
        barraLimpar.add(btnLimpar);
        painel.add(barraLimpar, BorderLayout.SOUTH);

        return painel;
    }

    // --- botoes e status ---

    private JPanel criarPainelInferior() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(COR_FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        painelBotoes.setBackground(COR_FUNDO);

        JButton btnCarregar = criarBotao("Carregar .asm", new Color(76, 175, 80));
        btnPasso = criarBotao("Passo", COR_DESTAQUE);
        btnExecutarTudo = criarBotao("Executar Tudo", COR_DESTAQUE);
        btnResetar = criarBotao("Resetar", new Color(244, 67, 54));

        btnCarregar.addActionListener(e -> carregarASM());
        btnPasso.addActionListener(this::acaoPasso);
        btnExecutarTudo.addActionListener(this::acaoExecutarTudo);
        btnResetar.addActionListener(e -> resetar());

        painelBotoes.add(btnCarregar);
        painelBotoes.add(Box.createHorizontalStrut(20));
        painelBotoes.add(btnPasso);
        painelBotoes.add(btnExecutarTudo);
        painelBotoes.add(Box.createHorizontalStrut(20));
        painelBotoes.add(btnResetar);

        painel.add(painelBotoes, BorderLayout.CENTER);

        labelStatus = new JLabel("Pronto - Carregue um programa .asm para iniciar");
        labelStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelStatus.setForeground(new Color(100, 100, 100));
        labelStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        painel.add(labelStatus, BorderLayout.SOUTH);

        return painel;
    }

    private JButton criarBotao(String texto, Color cor) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(cor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    // --- acoes ---

    // monta o .asm e joga na memoria
    private void carregarASM() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setDialogTitle("Selecionar programa assembly (.asm)");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Assembly SIC/XE (*.asm)", "asm"));
        int r = chooser.showOpenDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return;

        try {
            Path caminho = chooser.getSelectedFile().toPath();
            List<String> linhasFonte = Files.readAllLines(caminho);

            // macro
            boolean temMacro = false;
            for (String linha : linhasFonte) {
                String lt = linha.trim();
                if (lt.contains(" MACRO") || lt.equals("MEND")) {
                    temMacro = true;
                    break;
                }
            }

            if (temMacro) {
                log("[MACRO] Macros detectadas - processando...");
                linhasFonte = MainMacro.processarEmMemoria(linhasFonte);
                Files.write(java.nio.file.Paths.get("MASMAPRG.ASM"), linhasFonte);
                log("[MACRO] Macros expandidas - " + linhasFonte.size() + " linhas -> MASMAPRG.ASM");
            }

            // montagem
            log("[MONTADOR] Montando: " + caminho.getFileName());
            Montador montador = new Montador(linhasFonte);
            montador.primeiraPassagem();
            List<String> codigoHex = montador.segundaPassagem();

            // limpa obj antigos pra nao dar conflito
            java.io.File pastaObj = new java.io.File("objetos");
            if (pastaObj.exists()) {
                java.io.File[] antigos = pastaObj.listFiles((dir, name) -> name.toLowerCase().endsWith(".obj"));
                if (antigos != null) {
                    for (java.io.File f : antigos) f.delete();
                }
            }

            String nomeBase = caminho.getFileName().toString()
                    .replace(".asm", "").replace(".ASM", "");
            montador.salvarArquivoObjeto(nomeBase + ".obj", codigoHex);
            log("[MONTADOR] " + codigoHex.size() + " instrucoes - objeto salvo em objetos/" + nomeBase + ".obj");

            // ligacao
            java.io.File[] arquivosObj = pastaObj.listFiles((dir, name) -> name.toLowerCase().endsWith(".obj"));

            if (arquivosObj != null && arquivosObj.length > 0) {
                List<String> listaArquivos = new java.util.ArrayList<>();
                for (java.io.File f : arquivosObj) {
                    listaArquivos.add(f.getAbsolutePath());
                }
                java.util.Collections.sort(listaArquivos);

                boolean modoSimples = (listaArquivos.size() == 1);
                int enderecoCarga = modoSimples ? -1 : 0;

                Ligador ligador = new Ligador(enderecoCarga);
                log("[LIGADOR] " + listaArquivos.size() + " modulo(s) - "
                        + (modoSimples ? "modo simples" : "modo relocador"));
                ligador.primeiraPassagem(listaArquivos);
                List<String> codigoFinal = ligador.segundaPassagem();
                ligador.salvarArquivoFinal(codigoFinal, "executavel.obj");
                log("[LIGADOR] executavel.obj gerado");

                // carregamento
                reiniciarMaquina();
                Carregador carregador = new Carregador(maquina);

                int deslocamento = 0;
                if (carregador.isModoSimples("executavel.obj")) {
                    log("[CARREGADOR] Modo: Carregador Relocador");
                    String entrada = JOptionPane.showInputDialog(this,
                            "Endereco de carga (Hex):", "0000");
                    if (entrada != null && !entrada.trim().isEmpty()) {
                        deslocamento = Integer.parseInt(entrada.trim(), 16);
                    }
                } else {
                    log("[CARREGADOR] Modo: Carregador Absoluto");
                }

                int enderecoInicial = carregador.carregar("executavel.obj", deslocamento);
                if (enderecoInicial >= 0) {
                    maquina.getCpu().PC().setValor(enderecoInicial);
                }
                log(String.format("[CARREGADOR] Programa carregado (PC = %04X)", enderecoInicial >= 0 ? enderecoInicial : 0));
            } else {
                // se nao tem obj, carrega direto
                reiniciarMaquina();
                maquina.carregarProgramaHex(codigoHex, 0x0000);
                log("[CARREGADOR] Programa carregado direto na memoria (PC = 0000)");
            }

            this.caminhoPrograma = caminho;
            this.codigoHexCarregado = codigoHex;
            programaFinalizado = false;
            passoContador = 0;

            atualizarTudo();
            setStatus("Pronto para executar");
        } catch (Exception ex) {
            log("ERRO: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void acaoPasso(ActionEvent e) {
        if (programaFinalizado) {
            setStatus("Programa finalizado. Use Resetar para reiniciar.");
            return;
        }
        try {
            int pcAntes = maquina.getCpu().PC().getValorUnsigned();
            String descricao = maquina.passoVerboso();
            passoContador++;
            int pcDepois = maquina.getCpu().PC().getValorUnsigned();

            log(String.format("#%-4d %s", passoContador, descricao));
            atualizarTudo();

            if (pcDepois == 0 && pcAntes != 0) {
                programaFinalizado = true;
                log("========================================");
                log(">>> Programa finalizado (RSUB) apos " + passoContador + " passos");
                logEstadoFinal();
                setStatus("Programa finalizado apos " + passoContador + " passos");
            } else {
                setStatus("Passo " + passoContador + " | PC: " + String.format("%06X", pcDepois));
            }

            scrollParaPC();
        } catch (Exception ex) {
            programaFinalizado = true;
            atualizarTudo();
            log(">>> ERRO: " + ex.getMessage());
            setStatus("Erro na execucao: " + ex.getMessage());
        }
    }

    private void acaoExecutarTudo(ActionEvent e) {
        if (programaFinalizado) {
            setStatus("Programa finalizado. Use Resetar para reiniciar.");
            return;
        }
        if (executandoTudo) return;

        executandoTudo = true;
        btnPasso.setEnabled(false);
        btnExecutarTudo.setEnabled(false);
        setStatus("Executando...");
        log("--- Executando programa completo ---");

        new Thread(() -> {
            try {
                int maxPassos = 10000;
                List<String> linhasLog = new ArrayList<>();
                for (int i = 0; i < maxPassos; i++) {
                    int pcAntes = maquina.getCpu().PC().getValorUnsigned();
                    String descricao = maquina.passoVerboso();
                    passoContador++;
                    int pcDepois = maquina.getCpu().PC().getValorUnsigned();

                    linhasLog.add(String.format("#%-4d %s", passoContador, descricao));

                    if (pcDepois == 0 && pcAntes != 0) {
                        programaFinalizado = true;
                        final List<String> logFinal = new ArrayList<>(linhasLog);
                        SwingUtilities.invokeLater(() -> {
                            for (String l : logFinal) log(l);
                            log("========================================");
                            log(">>> Programa finalizado (RSUB) apos " + passoContador + " passos");
                            logEstadoFinal();
                            atualizarTudo();
                            setStatus("Programa finalizado apos " + passoContador + " passos");
                        });
                        break;
                    }
                }
                if (!programaFinalizado) {
                    final List<String> logFinal = new ArrayList<>(linhasLog);
                    SwingUtilities.invokeLater(() -> {
                        for (String l : logFinal) log(l);
                        atualizarTudo();
                        log(">>> Limite de " + maxPassos + " passos atingido");
                        setStatus("Limite de passos atingido (" + passoContador + " passos)");
                    });
                }
            } catch (Exception ex) {
                programaFinalizado = true;
                SwingUtilities.invokeLater(() -> {
                    atualizarTudo();
                    log(">>> ERRO: " + ex.getMessage());
                    setStatus("Erro na execucao: " + ex.getMessage());
                });
            } finally {
                executandoTudo = false;
                SwingUtilities.invokeLater(() -> {
                    btnPasso.setEnabled(true);
                    btnExecutarTudo.setEnabled(true);
                });
            }
        }).start();
    }

    private void logEstadoFinal() {
        CPU c = maquina.getCpu();
        log("--- Estado final dos registradores ---");
        log(String.format("  A  = %06X  (%d)", c.A().getValorUnsigned(), c.A().getValor()));
        log(String.format("  X  = %06X  (%d)", c.X().getValorUnsigned(), c.X().getValor()));
        log(String.format("  L  = %06X  (%d)", c.L().getValorUnsigned(), c.L().getValor()));
        log(String.format("  B  = %06X  (%d)", c.B().getValorUnsigned(), c.B().getValor()));
        log(String.format("  S  = %06X  (%d)", c.S().getValorUnsigned(), c.S().getValor()));
        log(String.format("  T  = %06X  (%d)", c.T().getValorUnsigned(), c.T().getValor()));
        log(String.format("  PC = %06X", c.PC().getValorUnsigned()));
        String ccStr = c.getCC() < 0 ? "LT (<)" : c.getCC() > 0 ? "GT (>)" : "EQ (=)";
        log(String.format("  SW = %s", ccStr));
        log("========================================");
    }

    private void resetar() {
        if (executandoTudo) return;

        reiniciarMaquina();
        passoContador = 0;
        areaSaida.setText("");

        if (caminhoPrograma != null && codigoHexCarregado != null) {
            maquina.carregarProgramaHex(codigoHexCarregado, 0x0000);
            atualizarTudo();
            log("Programa recarregado: " + caminhoPrograma.getFileName());
            setStatus("Programa resetado - pronto para executar");
        } else {
            atualizarTudo();
            log("Maquina reiniciada");
            setStatus("Maquina reiniciada - carregue um programa .asm");
        }
    }

    // --- atualizacao ---

    private void atualizarTudo() {
        atualizarRegistradores();
        modeloMemoria.fireTableDataChanged();
    }

    private void atualizarRegistradores() {
        CPU c = maquina.getCpu();
        setRegValor("A", c.A().getValorUnsigned());
        setRegValor("X", c.X().getValorUnsigned());
        setRegValor("L", c.L().getValorUnsigned());
        setRegValor("B", c.B().getValorUnsigned());
        setRegValor("S", c.S().getValorUnsigned());
        setRegValor("T", c.T().getValorUnsigned());
        setRegValor("PC", c.PC().getValorUnsigned());

        JLabel lblSW = labelsRegs.get("SW");
        if (lblSW != null) {
            int cc = c.getCC();
            String texto;
            if (cc < 0) texto = "LT (<)";
            else if (cc > 0) texto = "GT (>)";
            else texto = "EQ (=)";
            lblSW.setText(texto);
        }
    }

    private void setRegValor(String nome, int valor) {
        JLabel lbl = labelsRegs.get(nome);
        if (lbl != null) {
            lbl.setText(String.format("%06X", valor & 0xFFFFFF));
        }
    }

    private void scrollParaPC() {
        int pcRow = maquina.getCpu().PC().getValorUnsigned() / 16;
        if (pcRow >= 0 && pcRow < tabelaMemoria.getRowCount()) {
            tabelaMemoria.scrollRectToVisible(tabelaMemoria.getCellRect(pcRow, 0, true));
        }
    }

    private void log(String msg) {
        areaSaida.append(msg + "\n");
        areaSaida.setCaretPosition(areaSaida.getDocument().getLength());
    }

    private void setStatus(String msg) {
        labelStatus.setText(msg);
    }

    private void reiniciarMaquina() {
        maquina.getCpu().limparTodos();
        Memoria mem = maquina.getMemoria();
        for (int i = 0; i < mem.getTamanhoEmBytes(); i++) {
            mem.escreverByte(i, 0x00);
        }
        programaFinalizado = false;
    }

    // --- modelo da tabela de memoria ---

    private static class MemoriaTableModel extends AbstractTableModel {
        private final Memoria memoria;
        private final int bytesPerRow = 16;

        public MemoriaTableModel(Memoria memoria) {
            this.memoria = memoria;
        }

        @Override
        public int getRowCount() {
            return (memoria.getTamanhoEmBytes() + bytesPerRow - 1) / bytesPerRow;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) return "Endereco";
            else return "00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            int baseAddr = rowIndex * bytesPerRow;
            if (columnIndex == 0) {
                return String.format("%04X", baseAddr);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < bytesPerRow; i++) {
                    int addr = baseAddr + i;
                    if (addr < memoria.getTamanhoEmBytes()) {
                        sb.append(String.format("%02X", memoria.lerByte(addr)));
                    } else {
                        sb.append("  ");
                    }
                    if (i == 7) sb.append("  ");
                    else if (i < bytesPerRow - 1) sb.append(" ");
                }
                return sb.toString();
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    // --- main ---

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        SwingUtilities.invokeLater(() -> {
            InterfaceSimulador janela = new InterfaceSimulador();
            janela.setVisible(true);
        });
    }

    public Maquina getMaquina() {
        return maquina;
    }
}
