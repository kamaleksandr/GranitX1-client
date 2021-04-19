package granitX1;

/**
 * APaX-1EBAAAAVsbpdwIAifprghmOGn3YrQnrsNDX6BlgAugAAAAAAAAAAAC14RhFfs5r5KzQO3VrKrnXkyCa6Q==
 *
 * @author kamyshev.a
 */
public class GranitX1 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                //System.out.println(info.getName());
                //if ("Nimbus".equals(info.getName())) {
                //if ("Metal".equals(info.getName())) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
          
        }
        java.awt.EventQueue.invokeLater(() -> {
            new MainFrame().setVisible(true);
            
        });
        // TODO code application logic here
    }

}
