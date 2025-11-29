package executor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InterfaceSimulador extends JFrame {

    private final Maquina maquina;
    private final Path caminhoPrograma;
    private final int totalInstrucoes;

    private final Map<String, JTextField> camposRegs = new LinkedHashMap<>();
    private final TabelaMemoriaModel modeloMemoria;
    private final JTable tabelaMemoria;

    public InterfaceSimulador(Maquina maquina, Path caminhoPrograma, List<String> programaOriginal) {
        super("Simulador SIC/XE");

        this.maquina = maquina;
        this.caminhoPrograma = caminhoPrograma;
        this.totalInstrucoes = programaOriginal.size();

        this.maquina.carregarProgramaHex(programaOriginal, 0x0000);

        this.modeloMemoria = new TabelaMemoriaModel(maquina.getMemoria());
        this.tabelaMemoria = new JTable(modeloMemoria);

        inicializarComponentes();
        atualizarInterface();
    }

    private void inicializarComponentes() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Painel de registradores (topo)
        JPanel painelRegs = criarPainelRegistradores();
        add(painelRegs, BorderLayout.NORTH);

        // Painel de memória (centro)
        JScrollPane scrollMemoria = new JScrollPane(tabelaMemoria);
        add(scrollMemoria, BorderLayout.CENTER);

        // Painel de botões (baixo)
        JPanel painelBotoes = criarPainelBotoes();
        add(painelBotoes, BorderLayout.SOUTH);

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    // =========================
    //  Painel de registradores
    // =========================

    private JPanel criarPainelRegistradores() {
        JPanel painel = new JPanel(new GridLayout(2, 4, 8, 4));

        adicionarCampoReg(painel, "A");
        adicionarCampoReg(painel, "X");
        adicionarCampoReg(painel, "L");
        adicionarCampoReg(painel, "B");
        adicionarCampoReg(painel, "S");
        adicionarCampoReg(painel, "T");
        adicionarCampoReg(painel, "PC");
        adicionarCampoReg(painel, "CC");

        return painel;
    }

    private void adicionarCampoReg(JPanel painel, String labelTexto) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(labelTexto + ": ");
        JTextField txt = new JTextField(10);
        txt.setEditable(false);
        p.add(lbl, BorderLayout.WEST);
        p.add(txt, BorderLayout.CENTER);
        painel.add(p);

        camposRegs.put(labelTexto, txt);
    }

    // =========================
    //  Painel de botões
    // =========================

    private JPanel criarPainelBotoes() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton btnPasso = new JButton("Passo");
        JButton btnExecutarTudo = new JButton("Executar tudo");
        JButton btnReiniciar = new JButton("Reiniciar");

        btnPasso.addActionListener(this::acaoPasso);
        btnExecutarTudo.addActionListener(this::acaoExecutarTudo);
        btnReiniciar.addActionListener(e -> reiniciarExecucao());

        painel.add(btnPasso);
        painel.add(btnExecutarTudo);
        painel.add(btnReiniciar);

        return painel;
    }

    // =========================
    //  Ações dos botões
    // =========================

    private void acaoPasso(ActionEvent e) {
        try {
            maquina.passo();
            atualizarInterface();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao executar passo: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void acaoExecutarTudo(ActionEvent e) {
        // Executa EXATAMENTE o número de instruções do programa (uma por linha do .txt)
        new Thread(() -> {
            try {
                for (int i = 0; i < totalInstrucoes; i++) {
                    maquina.passo();
                    SwingUtilities.invokeLater(this::atualizarInterface);
                    Thread.sleep(5);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Execução interrompida: " + ex.getMessage(),
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE));
            }
        }).start();
    }

    private void reiniciarExecucao() {
        try {
            // Zera todos os registradores
            maquina.getCpu().limparTodos();

            // Zera toda a memória
            Memoria mem = maquina.getMemoria();
            for (int i = 0; i < mem.getTamanhoEmBytes(); i++) {
                mem.escreverByte(i, 0x00);
            }

            // Recarrega o programa do arquivo
            List<String> linhas = Files.readAllLines(caminhoPrograma);
            List<String> programa = new ArrayList<>();
            for (String linha : linhas) {
                linha = linha.trim();
                if (!linha.isEmpty()) {
                    if (linha.length() != 6) {
                        throw new RuntimeException("Linha inválida no arquivo: " + linha);
                    }
                    programa.add(linha);
                }
            }

            // Carrega novamente o programa a partir do endereço 0
            maquina.carregarProgramaHex(programa, 0x0000);

            atualizarInterface();

            JOptionPane.showMessageDialog(this,
                    "Execução reiniciada com sucesso!",
                    "Reiniciar",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao reiniciar: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================
    //  Atualização da interface
    // =========================

    private void atualizarInterface() {
        CPU c = maquina.getCpu();

        setTextoReg("A", c.A().getValorUnsigned());
        setTextoReg("X", c.X().getValorUnsigned());
        setTextoReg("L", c.L().getValorUnsigned());
        setTextoReg("B", c.B().getValorUnsigned());
        setTextoReg("S", c.S().getValorUnsigned());
        setTextoReg("T", c.T().getValorUnsigned());
        setTextoReg("PC", c.PC().getValorUnsigned());

        JTextField campoCC = camposRegs.get("CC");
        if (campoCC != null) {
            campoCC.setText(String.valueOf(c.getCC())); // -1, 0 ou 1
        }

        modeloMemoria.fireTableDataChanged();
    }

    private void setTextoReg(String nomeCampo, int valor) {
        JTextField campo = camposRegs.get(nomeCampo);
        if (campo != null) {
            campo.setText(String.format("%06X", valor & 0xFFFFFF));
        }
    }

    // =========================
    //  Modelo da tabela de memória
    // =========================

    private static class TabelaMemoriaModel extends AbstractTableModel {

        private final Memoria memoria;

        public TabelaMemoriaModel(Memoria memoria) {
            this.memoria = memoria;
        }

        @Override
        public int getRowCount() {
            return memoria.getTamanhoEmBytes();
        }

        @Override
        public int getColumnCount() {
            return 2; // Endereço, Valor
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) return "Endereço (byte)";
            else return "Valor (hex)";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return String.format("%06X", rowIndex);
            } else {
                int v = memoria.lerByte(rowIndex);
                return String.format("%02X", v);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    // =========================
    //  MAIN – lê programa.txt e abre a GUI
    // =========================

    public static void main(String[] args) throws Exception {
        // Arquivo de programa em hex (cada linha = 6 caracteres = 3 bytes)
        Path caminho = Paths.get("programa.txt");
        List<String> linhas = Files.readAllLines(caminho);
        List<String> programa = new ArrayList<>();

        for (String linha : linhas) {
            linha = linha.trim();
            if (!linha.isEmpty()) {
                if (linha.length() != 6) {
                    throw new RuntimeException("Linha inválida no arquivo: " + linha);
                }
                programa.add(linha);
            }
        }

        Maquina maquina = new Maquina(4096);

        SwingUtilities.invokeLater(() -> {
            InterfaceSimulador janela = new InterfaceSimulador(maquina, caminho, programa);
            janela.setVisible(true);
        });
    }
}
