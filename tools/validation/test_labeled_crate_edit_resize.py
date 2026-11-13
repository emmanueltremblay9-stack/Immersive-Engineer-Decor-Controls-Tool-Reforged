"""Execute production init with GUI doubles; not a native Minecraft resize test."""
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/LabeledCrateEditScreen.java'
ENTITY = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java'

FIXTURE = r'''
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
public class CrateResizeProbe {
    record Component(String key) { static Component translatable(String key) { return new Component(key); } }
    static class Mth { static int clamp(int n, int low, int high) { return Math.min(high, Math.max(low, n)); } }
    static class MachineBlockEntity { /* CONSTANTS */ }
    static class EditBox {
        String value = ""; int maxLength = 32; final int x, y, width, height;
        EditBox(Object font, int x, int y, int width, int height, Component label) {
            this.x=x; this.y=y; this.width=width; this.height=height;
        }
        void setMaxLength(int max) { maxLength=max; }
        void setValue(String text) { value=text.substring(0, Math.min(text.length(), maxLength)); }
        String getValue() { return value; }
    }
    static class Button {
        final Consumer<Button> action; Button(Consumer<Button> action) { this.action=action; }
        static Builder builder(Component label, Consumer<Button> action) { return new Builder(action); }
        static class Builder {
            final Consumer<Button> action; Builder(Consumer<Button> action) { this.action=action; }
            Builder bounds(int x, int y, int w, int h) { return this; }
            Button build() { return new Button(action); }
        }
    }
    int width=400, height=300, saves, closes; Object font=new Object(), focus;
    final String[] initialLines; EditBox firstLine, secondLine;
    final List<Object> widgets = new ArrayList<>();
    CrateResizeProbe(String first, String second) { initialLines=new String[]{first, second}; }
    void addRenderableWidget(Object widget) { widgets.add(widget); }
    void setInitialFocus(Object widget) { focus=widget; }
    void saveAndClose() { saves++; }
    void onClose() { closes++; }
    /* METHOD */
    void reinit(int w, int h) { width=w; height=h; widgets.clear(); init(); }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    void expect(String first, String second) {
        check(firstLine.getValue().equals(first), "first line lost");
        check(secondLine.getValue().equals(second), "second line lost");
        check(widgets.size()==4 && widgets.get(0)==firstLine && widgets.get(1)==secondLine, "widgets replaced");
        check(focus==firstLine, "initial focus retained");
        check(saves==0 && closes==0, "initialization must not save or close");
        check(firstLine.maxLength==MachineBlockEntity.LABEL_MAX_UTF16_UNITS &&
              secondLine.maxLength==MachineBlockEntity.LABEL_MAX_UTF16_UNITS, "max length unchanged");
    }
    public static void main(String[] args) {
        CrateResizeProbe p=new CrateResizeProbe("stored A", "stored B");
        p.reinit(400,300); p.expect("stored A", "stored B");
        p.firstLine.setValue("Cuivre électricité 🛠"); p.secondLine.setValue("");
        EditBox previous=p.firstLine;
        p.reinit(800,450); p.expect("Cuivre électricité 🛠", "");
        check(p.firstLine!=previous && p.firstLine.x!=previous.x, "resize recreates and repositions widgets");
        p.firstLine.setValue(""); p.secondLine.setValue("Deuxième réserve 🧰");
        for (int[] size : new int[][]{{320,240},{1920,1080},{854,480},{640,360},{400,300}}) {
            p.reinit(size[0], size[1]); p.expect("", "Deuxième réserve 🧰");
        }
        int max=MachineBlockEntity.LABEL_MAX_UTF16_UNITS;
        p.firstLine.setValue("x".repeat(max+7)); p.secondLine.setValue("y".repeat(max));
        p.reinit(320,240); p.expect("x".repeat(max), "y".repeat(max));
        check(p.initialLines[0].equals("stored A") && p.initialLines[1].equals("stored B"), "opening values not mutated");
        CrateResizeProbe other=new CrateResizeProbe("other crate", "new stored value");
        other.reinit(640,360); other.expect("other crate", "new stored value");
        CrateResizeProbe reopened=new CrateResizeProbe("stored A", "stored B");
        reopened.reinit(400,300); reopened.expect("stored A", "stored B");
        System.out.println("Draft text retained over reinitialization; isolated screens, lengths and no-save verified");
    }
}
'''


def run_probe(source):
    start = source.index('   protected void init() {')
    end = source.index('\n   public void render(', start)
    entity = ENTITY.read_text(encoding='utf-8')
    constants = '\n'.join(re.findall(r'public static final int LABEL_MAX_(?:LINE_LENGTH|UTF16_UNITS) = [^;]+;', entity))
    if len(constants.splitlines()) != 2:
        raise AssertionError('Expected the two production length constants')
    java_source = FIXTURE.replace('/* METHOD */', source[start:end]).replace('/* CONSTANTS */', constants)
    with tempfile.TemporaryDirectory(prefix='crate-resize-') as directory:
        target = Path(directory)
        java = target / 'CrateResizeProbe.java'
        java.write_text(java_source, encoding='utf-8')
        compiled = subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-encoding', 'UTF-8',
                                   '-source', '21', '-target', '21', '-d', str(target), str(java)],
                                  capture_output=True, text=True, timeout=60)
        if compiled.returncode:
            raise AssertionError(compiled.stderr)
        return subprocess.run(['java', '-cp', str(target), 'CrateResizeProbe'],
                              capture_output=True, text=True, timeout=30)


class LabeledCrateEditResizeTests(unittest.TestCase):
    def test_production_init_retains_draft_without_saving(self):
        result = run_probe(SCREEN.read_text(encoding='utf-8'))
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_original_two_line_reset_is_rejected(self):
        source = SCREEN.read_text(encoding='utf-8').replace('this.firstLine.setValue(line0);', 'this.firstLine.setValue(this.initialLines[0]);').replace('this.secondLine.setValue(line1);', 'this.secondLine.setValue(this.initialLines[1]);')
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('first line lost', result.stderr)

    def test_second_line_only_reset_is_rejected(self):
        source = SCREEN.read_text(encoding='utf-8').replace('this.secondLine.setValue(line1);', 'this.secondLine.setValue(this.initialLines[1]);')
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('second line lost', result.stderr)

    def test_crossed_lines_are_rejected(self):
        source = SCREEN.read_text(encoding='utf-8').replace('this.secondLine.getValue();', 'this.firstLine.getValue();')
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('second line lost', result.stderr)

    def test_save_during_reinit_is_rejected(self):
        source = SCREEN.read_text(encoding='utf-8').replace('this.setInitialFocus(this.firstLine);', 'this.setInitialFocus(this.firstLine); this.saveAndClose();')
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('initialization must not save or close', result.stderr)


if __name__ == '__main__':
    unittest.main()
