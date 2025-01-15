package mod.hilal.saif.lib;

import a.a.a.Zx;
import android.widget.TextView;

public class PCP implements Zx.b {

  private final TextView toSetText;

  public PCP(TextView toSetText) {
    this.toSetText = toSetText;
  }

  @Override
  public void a(int color) {
    toSetText.setText(String.format("#%08X", color));
  }

  @Override
  public void a(String color, int i2) {
    toSetText.setText(String.format("#%08X", color));
  }
}
