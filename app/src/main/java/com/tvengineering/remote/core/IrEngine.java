package com.tvengineering.remote.core;

import android.content.Context;
import android.hardware.ConsumerIrManager;

import com.tvengineering.remote.data.IrCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IrEngine {
    private final ConsumerIrManager manager;

    public IrEngine(Context context) {
        manager = (ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
    }

    public boolean hasEmitter() {
        return manager != null && manager.hasIrEmitter();
    }

    public String carrierRangesText() {
        if (!hasEmitter()) return "No IR emitter detected";
        ConsumerIrManager.CarrierFrequencyRange[] ranges = manager.getCarrierFrequencies();
        if (ranges == null || ranges.length == 0) return "IR emitter detected; carrier ranges not reported";
        StringBuilder sb = new StringBuilder();
        for (ConsumerIrManager.CarrierFrequencyRange r : ranges) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(r.getMinFrequency()).append("-").append(r.getMaxFrequency()).append(" Hz");
        }
        return sb.toString();
    }

    public void transmit(IrCommand command) {
        if (!hasEmitter()) throw new IllegalStateException("This Android device does not expose a Consumer IR emitter.");
        int carrier = command.carrierHz <= 0 ? 38000 : command.carrierHz;
        int[] pattern;
        switch (command.type) {
            case NEC:
                pattern = nec(command.address, command.command);
                break;
            case PULSE32:
                pattern = pulseDistance32(command.data32);
                break;
            case PRONTO:
                ProntoResult pr = parsePronto0000(command.pronto);
                carrier = pr.carrierHz;
                pattern = pr.pattern;
                break;
            case RAW:
            default:
                pattern = command.rawPattern;
                break;
        }
        validate(carrier, pattern);
        manager.transmit(carrier, pattern);
    }

    public static void validate(int carrier, int[] pattern) {
        if (carrier < 20000 || carrier > 100000) throw new IllegalArgumentException("Carrier must be between 20 kHz and 100 kHz.");
        if (pattern == null || pattern.length < 2) throw new IllegalArgumentException("IR pattern is empty.");
        long total = 0;
        for (int v : pattern) {
            if (v <= 0) throw new IllegalArgumentException("Every mark/space duration must be > 0 microseconds.");
            total += v;
        }
        if (total >= 2_000_000L) throw new IllegalArgumentException("Android requires each IR transmission pattern to be shorter than 2 seconds.");
    }

    public static int[] nec(int address, int command) {
        int addr = address & 0xFF;
        int cmd = command & 0xFF;
        long frame = (long) addr |
                ((long) ((~addr) & 0xFF) << 8) |
                ((long) cmd << 16) |
                ((long) ((~cmd) & 0xFF) << 24);
        List<Integer> p = new ArrayList<>();
        p.add(9000); p.add(4500);
        for (int i = 0; i < 32; i++) {
            p.add(560);
            p.add(((frame >>> i) & 1L) == 1L ? 1690 : 560);
        }
        p.add(560);
        return toArray(p);
    }

    public static int[] pulseDistance32(long data) {
        List<Integer> p = new ArrayList<>();
        p.add(4500); p.add(4500);
        for (int i = 31; i >= 0; i--) {
            p.add(560);
            p.add(((data >>> i) & 1L) == 1L ? 1690 : 560);
        }
        p.add(560);
        return toArray(p);
    }

    private static int[] toArray(List<Integer> p) {
        int[] out = new int[p.size()];
        for (int i = 0; i < p.size(); i++) out[i] = p.get(i);
        return out;
    }

    public static int[] parseRaw(String text) {
        String clean = text == null ? "" : text.trim().replace('[', ' ').replace(']', ' ');
        if (clean.isEmpty()) return new int[0];
        String[] parts = clean.split("[,\\s]+");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    public static long parseLongFlexible(String value) {
        String s = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (s.startsWith("0x")) return Long.parseUnsignedLong(s.substring(2), 16);
        return Long.parseLong(s);
    }

    public static int parseIntFlexible(String value) {
        long v = parseLongFlexible(value);
        return (int) v;
    }

    public static class ProntoResult {
        public final int carrierHz;
        public final int[] pattern;
        public ProntoResult(int carrierHz, int[] pattern) { this.carrierHz = carrierHz; this.pattern = pattern; }
    }

    public static ProntoResult parsePronto0000(String pronto) {
        if (pronto == null) throw new IllegalArgumentException("Pronto text is empty.");
        String[] parts = pronto.trim().split("\\s+");
        if (parts.length < 6) throw new IllegalArgumentException("Invalid Pronto 0000 sequence.");
        int[] words = new int[parts.length];
        for (int i = 0; i < parts.length; i++) words[i] = Integer.parseInt(parts[i], 16);
        if (words[0] != 0x0000) throw new IllegalArgumentException("Only learned Pronto format 0000 is supported.");
        if (words[1] == 0) throw new IllegalArgumentException("Invalid Pronto frequency word.");
        double carrier = 1_000_000.0 / (words[1] * 0.241246);
        double periodUs = 1_000_000.0 / carrier;
        int introPairs = words[2];
        int repeatPairs = words[3];
        int availablePairs = Math.max(0, (words.length - 4) / 2);
        int pairs = Math.min(availablePairs, introPairs > 0 ? introPairs : repeatPairs);
        if (pairs == 0) pairs = availablePairs;
        int[] pattern = new int[pairs * 2];
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = Math.max(1, (int) Math.round(words[4 + i] * periodUs));
        }
        return new ProntoResult((int) Math.round(carrier), pattern);
    }
}
