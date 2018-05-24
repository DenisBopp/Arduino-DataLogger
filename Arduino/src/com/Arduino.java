
/***********************************************************************************************
 *                                  Arduino Data Plotter/Logger
 * Objetivo: Esta aplicação tem por objetivo plotar os dados enviados pela porta serial do Arduino
 *  Os dados devem ser enviados como uma sequencia de caracteres, a configuração pode ser ajustada
 *  de acordo com a conveniência do usuário, mas deve ser feita tanto no envio pelo Arduino, como
 *  nesta aplicação na captura dos dados
 *  
 *  Ver instruções nas linhas: 85, 96, 107, 140, 149, 166, 199, 212 e 215
 *    
 *                                  
 * Autor: Denis Augusto Bopp da Silva 
 * Universidade Federal do Rio Grande do Sul
 * Data: 22/05/2018
 * Versão: 2.0
 * email: denis.bopp@ufrgs.br; denisbopp@netscape.net
 **********************************************************************************************/


package com;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.lang.Thread;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.fazecast.jSerialComm.SerialPort;

public class Arduino extends JFrame implements Runnable{

	private FileWriter file;
	private PrintWriter pfile;
		
	int n = 0;	
	private static SerialPort porta = null;
	private static final long serialVersionUID = 1;
	private static final int Width = 1024;
	private static final int Height = 650;
	static int i;

	Date data = new Date();
	SimpleDateFormat formato1 = new SimpleDateFormat("hh:mm:ss");
	SimpleDateFormat formato2 = new SimpleDateFormat("YYYYMMd_hhmmss");
	String tempo;
	String valor;
	Double t1 = 0.0, t2 = 0.0, t3 = 0.0, t4 = 0.0, ta = 0.0, ur = 0.0;

	public static void main(String[] args) {
		//setDefaultLookAndFeelDecorated(true);
		(new Thread(new Arduino())).start();
	}
	
	public Arduino() {

		//Declaração dos objetos
		Toolkit tk = Toolkit.getDefaultToolkit();
		SpringLayout sp = new SpringLayout();
		JPanel panel = new JPanel();
		
		/*
		 *  Adicionar as séries que deve aparecer no gráfico
		 * Modelo: TimeSeries "Nome da Serie" = new TimeSeries("Legenda", Second.class);
		 * 
		 */
		TimeSeries serieTA = new TimeSeries("Temp Amb", Second.class);
		TimeSeries serieT1 = new TimeSeries("Temp T1", Second.class);
		TimeSeries serieT2 = new TimeSeries("Temp T2", Second.class);
		TimeSeries serieT3 = new TimeSeries("Temp T3", Second.class);
		TimeSeries serieUR = new TimeSeries("Umidade Relativa", Second.class);
		
		/*
		 *  Adicionar a nova série ao dataset
		 *  Modelo: dataset.addSeries("Nome da Série");
		 */
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(serieTA);
		dataset.addSeries(serieT1);
		dataset.addSeries(serieT2);
		dataset.addSeries(serieT3);
		dataset.addSeries(serieUR);
		
		/*
		 * Personalizar o gráfico na função abaixo
		 * 1º - Título do gráfico
		 * 2º - Eixo X
		 * 3º - Eixo Y
		 */
		JFreeChart chart = ChartFactory.createTimeSeriesChart("Temperatura x Tempo", "Tempo", "Temperatura", dataset);
		ChartPanel chartPanel = new ChartPanel(chart);
		Container container = this.getContentPane();
		
		XYPlot plot = chart.getXYPlot();
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(formato1);
		NumberAxis range = (NumberAxis)plot.getRangeAxis();
		range.setAutoRange(true);
		
		//Cria o combo com as portas seriais disponíveis
		JComboBox<String> cbSerial = new JComboBox<String>();
		SerialPort[] portas = SerialPort.getCommPorts();
		for(SerialPort porta: portas) {
			cbSerial.addItem(porta.getSystemPortName());
		}
		
		//Cria o botão de conexão e adiciona o botão para iniciar a troca de dados com o arduino
		JButton btAcao = new JButton("Conectar");
		btAcao.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(btAcao.getText()=="Conectar") {
					btAcao.setText("Desconectar");
					cbSerial.setEnabled(false);
					try {	
						
						/*
						 * Cria o arquivo para salvar os dados
						 * 
						 */
						String arquivo = "Dados_Coletados_em_" + formato2.format(data) + "_.csv";
						System.out.println(arquivo);
						file = new FileWriter(arquivo);
						pfile = new PrintWriter(file);
						
						/*
						 * O formato de gravação deve ser feito de acordo com os dados
						 *  a separação é feita por tabulação, mas pode ser feita com 
						 *  virgula, ponto e virgula ou outros separadores
						 */
						pfile.print("TA ºC\tT1 ºC\tT2 ºC\tT3 ºC\tUR \045  \tHora\n");
						
						//Faz a conexão com a porta serial
						String tty = cbSerial.getSelectedItem().toString(); //Pega o nome da porta serial do combobox
						porta = SerialPort.getCommPort(tty);				//Configura a porta
						porta.setBaudRate(9600);							//Configura o BaudRate
						porta.setNumDataBits(8);							//Configura o número de bits de dados
						porta.setNumStopBits(1);							//Configura o número de bits de parada
						porta.setFlowControl(0);							//Configura o tipo de fluxo de controle 0-> desabilitado 
						porta.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);	//Define os time outs
						porta.openPort();									//Abre a conexão

						/*
						 * Limpa o gráfico sempre que houver uma nova conexão, as séries de dados são
						 * excluidas para novos dados, cada serie adicionada deve ser colocada (ou excluída) 
						 *
						 */
						serieTA.clear();
						serieT1.clear();
						serieT2.clear();
						serieT3.clear();
						serieUR.clear();

						if(porta.isOpen()) { //Verifica se a porta está aberta
							System.out.println("Porta " + tty + " aberta!"); //Escreve uma mensagem no console
							Thread thread = new Thread() { //Inicia uma nova tarefa que vai executar as leituras na porta serial
								@Override
								public void run() {
									//Cria o objeto que obtem os dados vindos da porta serial
									Scanner scanner = new Scanner(porta.getInputStream()); //O formato dos dados aqui é três algarismos para cada dado enviado
									while(scanner.hasNextLine()) {
										try {
											String valor = scanner.nextLine(); //Salva os dados em uma String
											if(n==0) { //Pula a primeira leitura, pois esta pode vir truncada
												n++;
											}else 
											{	//Apenas para verificar ser os valores estão vindo corretamente
												System.out.println(valor);
												System.out.println((Double.parseDouble(valor.substring(0, 3))/10));
												System.out.println((Double.parseDouble(valor.substring(3, 6))/10));
												System.out.println((Double.parseDouble(valor.substring(6, 9))/10));
												System.out.println((Double.parseDouble(valor.substring(9, 12))/10));
												System.out.println((Double.parseDouble(valor.substring(12, 15))/10));
												//System.out.println((Double.parseDouble(valor.substring(15, 18))/10));

												//Separa a String que veio da porta serial entre as variáveis e converte para decimal
												t1 = Double.parseDouble(valor.substring(0, 3))/10;
												ta = Double.parseDouble(valor.substring(3, 6))/10;
												t3 = Double.parseDouble(valor.substring(6, 9))/10;
												t2 = Double.parseDouble(valor.substring(9, 12))/10;
												ur = Double.parseDouble(valor.substring(12, 15))/10;
												
												//Pega a hora atual
												tempo = formato1.format(System.currentTimeMillis());
												Second s = new Second();
												
												//System.out.println(tempo);
												
												//Insere os dados no arquivo de texto
												pfile.printf("%4.1f\t%4.1f\t%4.1f\t%4.1f\t%4.1f\t%s\n", ta, t1, t3, t2, ur, tempo.toString());
												
												//Insere os dados no gráfico
												/*
												 *  Para cada nova série de dados que for inserida
												 *  uma nova entrada deve ser adicionada ao gráfico
												 *  
												 */
												serieTA.add(s, ta); //Adiciona o tempo e o valor [(s, v)]
												serieT1.add(s, t1);
												serieT2.add(s, t2);
												serieT3.add(s, t3);
												serieUR.add(s, ur);
											}									
										}catch(Exception e1) {
											System.out.println(e1);
										}
									}
									scanner.close();//Fecha o objeto de leitura da serial
								}
							};
							thread.start();//Inicia a tarefa de leitura
						}
					}catch(Exception ez) {
						System.out.println(ez);
					}
					System.out.println("Porta aberta!");
				}else {
					try {
						porta.closePort();//Fecha a porta serial
						file.close(); //Fecha o arquivo
						pfile.close();
					}catch(Exception exx) {System.out.println(exx);}
					if(!porta.isOpen()) {						
						n = 0;
						System.out.println("Porta serial fechada!");
						btAcao.setText("Conectar");
						cbSerial.setEnabled(true);	
					}else {System.out.println("Erro!");}
				}
			}	
		});
		
		//Cria o botão de sair e adiciona a ação de fechar o aplicativo
		JButton btSair = new JButton("Sair");
		btSair.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(DISPOSE_ON_CLOSE);
			}	
		});

		//Adiciona os componentes ao layout
		container.setLayout(sp);
		container.add(chartPanel);
		container.add(cbSerial);
		container.add(btAcao);
		container.add(btSair);

		//Dimensiona os componentes
		cbSerial.setMaximumSize(new Dimension(200, 100));
		cbSerial.setPreferredSize(new Dimension(200, 30));

		btAcao.setMaximumSize(new Dimension(200, 100));
		btAcao.setPreferredSize(new Dimension(200, 30));

		btSair.setMaximumSize(new Dimension(200, 100));
		btSair.setPreferredSize(new Dimension(200, 30));
		
		chartPanel.setMaximumSize(new Dimension(Width-(245), Height -(60)));
		chartPanel.setPreferredSize(new Dimension(Width-(230), Height -(50)));
		
		//Posiciona os componentes na tela
		sp.putConstraint(SpringLayout.WEST, chartPanel, 15, SpringLayout.WEST, container);   	
		sp.putConstraint(SpringLayout.NORTH, chartPanel, 15, SpringLayout.NORTH, container);

		sp.putConstraint(SpringLayout.EAST, cbSerial, getSize().width-(cbSerial.getSize().width+5), SpringLayout.EAST, container);
		sp.putConstraint(SpringLayout.NORTH, cbSerial, 15, SpringLayout.NORTH, container);

		sp.putConstraint(SpringLayout.EAST, btAcao, getSize().width-(btAcao.getSize().width+5), SpringLayout.EAST, container);
		sp.putConstraint(SpringLayout.NORTH, btAcao, 5, SpringLayout.SOUTH, cbSerial);

		sp.putConstraint(SpringLayout.EAST, btSair, getSize().width-(btSair.getSize().width+5), SpringLayout.EAST, container);
		sp.putConstraint(SpringLayout.NORTH, btSair, 5, SpringLayout.SOUTH, btAcao);

		this.getContentPane().addComponentListener(new ComponentListener(){

			@Override
			public void componentResized(ComponentEvent e) {
				// TODO Auto-generated method stub
				//Redimensiona o gráfico de acordo com o tamanho da tela
				chartPanel.setSize(new Dimension(new Dimension(getWidth()-230, getHeight()-50)));	
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		
		//Construção da janela
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(Width, Height);
		this.setLocation((tk.getScreenSize().width - Width)/2, (tk.getScreenSize().height - Height)/2);;
		this.setVisible(true);
		System.out.println(chartPanel.getSize().height);
		System.out.println(chartPanel.getSize().width);

	}

	@Override
	public void run() {

	}
}
