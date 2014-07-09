package p2pp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.Border;

import org.klomp.snark.Peer;
import org.klomp.snark.PeerCoordinator;

public class GUIDownloadProgressListener extends JComponent 
		implements ComponentListener, DownloadProgressListener {
	
	private JLabel text;
	private JLabel progress;
	private Timer timer;

	private double nbOfPieces;
	private int piecesDownloaded = 0;
	private PeerCoordinator coord;
	private Map<Integer, Color> pieces = new HashMap<Integer, Color>();
	private double barWidth;
	private double playbackPosition = -1;
	
	private final int BAR_HEIGHT = 30;
	private final int INIT_BAR_WIDTH = 1024;
	private final int X = 10;
	private final int Y = 10;
	
	public GUIDownloadProgressListener(int nbOfPieces, 
			final double bitrate, final int wait) {
		this(nbOfPieces);

		timer = new Timer(100, new ActionListener() {
			
			private long start = 0;
			
			@Override
			public void actionPerformed(ActionEvent event) {
				if(start == 0)
					start = System.currentTimeMillis();
				
				double speed = bitrate * 
					((System.currentTimeMillis() - start) / 1000.0);

				playbackPosition = speed * barWidth / 
				GUIDownloadProgressListener.this.nbOfPieces;
				//System.out.println("playback=" + playbackPosition);

				if(playbackPosition >= barWidth) {
					playbackPosition = barWidth;
					timer.stop();
				}

				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						GUIDownloadProgressListener.this.repaint();
					}
				});
			}
		});
		
		timer.setInitialDelay(wait * 1000);
		timer.start();
	}

	public GUIDownloadProgressListener(int nbOfPieces) {
		this.nbOfPieces = nbOfPieces;
		
		final JFrame frame = new JFrame("Piece Download Progress");
		frame.getContentPane().add(this, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addComponentListener(this);
		
		text = new JLabel("Initialized");
		Border paddingBorder = BorderFactory.createEmptyBorder(0,10,5,0);
		text.setBorder(paddingBorder);
		frame.getContentPane().add(text, BorderLayout.SOUTH);
		
		progress = new JLabel("0%");
		progress.setPreferredSize(new Dimension(50, 50));
		progress.setBorder(paddingBorder);
		frame.getContentPane().add(progress, BorderLayout.WEST);
		
		JPanel legends = new JPanel(new FlowLayout()); 
		legends.add(new Legend(Color.BLUE, "Got piece from peer"), 
				BorderLayout.NORTH);
		legends.add(new Legend(Color.GREEN, "Got piece from server"), 
				BorderLayout.NORTH);
		legends.add(new Legend(Color.RED, "Piece requested"), 
				BorderLayout.NORTH);
		frame.getContentPane().add(legends, BorderLayout.NORTH);
		
		this.barWidth = INIT_BAR_WIDTH;
		this.setPreferredSize(new Dimension(INIT_BAR_WIDTH + 
				2 * X, BAR_HEIGHT + 2 * Y));
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
	
	@Override
	public void pieceRequested(Peer peer, int piece) {
		synchronized(pieces) {
			if(pieces.containsKey(piece))
				return;
			
			pieces.put(piece, Color.RED);
			
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					GUIDownloadProgressListener.this.repaint();
				}
			});
		}
	}
	
	@Override
	public void downloadComplete() {
		coord.halt();
		
		synchronized(text) {
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					text.setText("Download complete!");
				}
			});
		}
	}
	
	@Override
	public void setPeerCoordinator(PeerCoordinator coord) {
		this.coord = coord;
	}
	
	@Override
	public void pieceDownloaded(final Peer peer, final int piece) {
		synchronized(pieces) {
			Color color = peer == null ? Color.GREEN : Color.BLUE;
			
			piecesDownloaded++;
			pieces.put(piece, color);
			
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					String from = peer == null ? WebSeed.description : peer.toString();
					text.setText("Recieved piece " + piece + 
							" from " + from + "!");
					
					double prog = ((double) piecesDownloaded) / nbOfPieces;
					progress.setText(((int) (prog * 100)) + "%");
					
					GUIDownloadProgressListener.this.repaint();
				}
			});
		}
	}
	
	@Override
	public void paintComponent(Graphics g) {
		//g.drawImage(background, -100, -320, null);
		super.paintComponents(g);
	}
	
	@Override
	public void paint(Graphics g) {
		synchronized(pieces) {
			super.paint(g);
			
			Graphics2D g2 = (Graphics2D) g;
			
			Rectangle2D.Double bar = new Rectangle2D.Double(X, Y, 
					barWidth, BAR_HEIGHT);
			g2.setColor(Color.WHITE);
			g2.fill(bar);
			
			if(!pieces.isEmpty()) {
				double pieceWidth = barWidth / nbOfPieces;
				ArrayList<Integer> keys = new ArrayList<Integer>(pieces.keySet());
				Collections.sort(keys);
		
				int first = keys.get(0);
				int currentPiece = first;
				Color currentColor = pieces.get(first);
				int currentPieceCount = 0;
		
				for(int piece : keys) {
					Color color = pieces.get(piece);
					if(color.equals(currentColor) && currentPiece++ == piece)
						currentPieceCount++;
					else {
						drawPieces(g2, pieceWidth, currentColor, first, 
								currentPiece, currentPieceCount);
						
						currentColor = pieces.get(piece);
						currentPieceCount = 1;
						first = piece;
						currentPiece = piece + 1;
					}
				}
				
				drawPieces(g2, pieceWidth, currentColor, first, 
						currentPiece, currentPieceCount);
			}
			
			Line2D.Double play = new Line2D.Double(X + playbackPosition, 
				Y, X + playbackPosition, Y + BAR_HEIGHT);
			
			g2.setColor(Color.BLACK);
			g2.draw(play);
			g2.draw(bar);
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		Component frame = e.getComponent();
		barWidth = ((JFrame) frame).getContentPane().getWidth() - 
			2 * X - progress.getWidth();
	}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentHidden(ComponentEvent e) {}
	
	private void drawPieces(Graphics2D g2, double pieceWidth, 
			Color currentColor, int first,
			int currentPiece, int currentPieceCount) {
		double x = X + first * pieceWidth;
		double width = pieceWidth * currentPieceCount;
		Rectangle2D.Double mark = new Rectangle2D.Double(x, Y, 
				width, BAR_HEIGHT);
		g2.setColor(currentColor);
		g2.fill(mark);
		g2.setColor(new Color(0,0,0,0));
		g2.draw(mark);
	}
	
	static class Legend extends JLabel {
		
		public Legend(Color color, String text) {
			//this.color = color;
			//this.text = text;
			Border paddingBorder = BorderFactory.createEmptyBorder(0,10,0,10);
			this.setBorder(paddingBorder);
			
			this.setIcon(makeIcon(color, 10));
			this.setText(text);
		}
		
		private Icon makeIcon(final Color color, final int r) {
			return new Icon() {	
				@Override
				public void paintIcon(Component c, Graphics g, int x, int y) {
					Graphics2D g2 = (Graphics2D) g;
					Ellipse2D.Double circle = new Ellipse2D.Double(
							x, y, r, r);
					g2.setColor(color);
					g2.fill(circle);
					g2.draw(circle);
				}
				
				@Override
				public int getIconWidth() {
					return r;
				}
				
				@Override
				public int getIconHeight() {
					return r;
				}
			};
		}
		
	}
	
}
