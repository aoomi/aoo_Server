package com.aoo.bcg.mahjong;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Small deterministic rule components shared by regional Mahjong providers. */
public final class MahjongRegionalRules {
    private MahjongRegionalRules() {}

    public enum DealerOutcome { WIN, DRAW, LOSS }
    public record DealerPolicy(boolean retainOnWin, boolean retainOnDraw) {
        /** Selects the initial dealer from an already sorted seat list and an authoritative random offset. */
        public int initialDealer(List<Integer> sortedSeats,int authoritativeOffset){if(sortedSeats.size()<2||sortedSeats.size()!=new HashSet<>(sortedSeats).size()||!sortedSeats.equals(sortedSeats.stream().sorted().toList()))throw new IllegalArgumentException("initial dealer seats must be unique and sorted");return sortedSeats.get(Math.floorMod(authoritativeOffset,sortedSeats.size()));}
        public int nextDealer(int dealer, DealerOutcome outcome, List<Integer> seats) {
            if (outcome==null || !seats.contains(dealer) || seats.size() < 2 || seats.size()!=new HashSet<>(seats).size()) throw new IllegalArgumentException("invalid seats/dealer/outcome");
            if ((outcome == DealerOutcome.WIN && retainOnWin) || (outcome == DealerOutcome.DRAW && retainOnDraw)) return dealer;
            List<Integer> ordered = seats.stream().distinct().sorted().toList();
            return ordered.get((ordered.indexOf(dealer) + 1) % ordered.size());
        }
    }

    public record ReplacementEvent(int seat, int flowerTile, int replacementTile) {}
    public record DealResult(Map<Integer,List<Integer>> hands, List<Integer> wall, List<Integer> flowers,
            List<ReplacementEvent> replacements) {}
    public static DealResult deal(MahjongTileSet tileSet, List<Integer> wall, List<Integer> seats, int dealer,
            int normalHandSize, boolean replaceFlowers) {
        if (!seats.contains(dealer) || normalHandSize < 1 || seats.size()!=new HashSet<>(seats).size()) throw new IllegalArgumentException("invalid deal request");
        ArrayList<Integer> remaining = new ArrayList<>(wall);
        if (!tileSet.inventoryViolations(remaining).isEmpty()) throw new IllegalArgumentException("invalid wall inventory");
        LinkedHashMap<Integer,List<Integer>> hands = new LinkedHashMap<>();
        ArrayList<Integer> flowers = new ArrayList<>();
        ArrayList<ReplacementEvent> replacements = new ArrayList<>();
        for (int seat : seats) {
            int wanted = normalHandSize + (seat == dealer ? 1 : 0);
            ArrayList<Integer> hand = new ArrayList<>();
            while (hand.size() < wanted) {
                if (remaining.isEmpty()) throw new IllegalStateException("wall exhausted while dealing");
                int tile = remaining.remove(0);
                if (replaceFlowers && tileSet.isFlower(tile)) {
                    flowers.add(tile);
                    if (remaining.isEmpty()) throw new IllegalStateException("wall exhausted while replacing flower");
                    int replacement = remaining.get(0);
                    replacements.add(new ReplacementEvent(seat,tile,replacement));
                } else hand.add(tile);
            }
            hands.put(seat, List.copyOf(hand));
        }
        return new DealResult(Map.copyOf(hands), List.copyOf(remaining), List.copyOf(flowers),List.copyOf(replacements));
    }

    /** Serializable opening choices; timeout choices use stable tile/suit ordering. */
    public record OpeningSelections(Map<Integer,List<Integer>> exchanges, Map<Integer,MahjongSuit> missingSuits) {
        public OpeningSelections { exchanges=Map.copyOf(exchanges);missingSuits=Map.copyOf(missingSuits); }
        public static OpeningSelections empty(){return new OpeningSelections(Map.of(),Map.of());}
        public OpeningSelections submitExchange(int seat,List<Integer> hand,List<Integer> tiles){if(exchanges.containsKey(seat))throw new IllegalStateException("exchange already submitted");MahjongSuit suit=tiles.isEmpty()?null:java.util.Arrays.stream(MahjongSuit.values()).filter(s->s.contains(tiles.get(0))).findFirst().orElse(null);if(tiles.size()!=3||suit==null||tiles.stream().anyMatch(t->!suit.contains(t)))throw new IllegalArgumentException("exchange requires three suited same-suit tiles");ArrayList<Integer>copy=new ArrayList<>(hand);for(int tile:tiles)if(!copy.remove(Integer.valueOf(tile)))throw new IllegalArgumentException("exchange tile absent");Map<Integer,List<Integer>>next=new LinkedHashMap<>(exchanges);next.put(seat,List.copyOf(tiles));return new OpeningSelections(next,missingSuits);}
        public OpeningSelections submitMissingSuit(int seat,MahjongSuit suit){if(missingSuits.containsKey(seat))throw new IllegalStateException("missing suit already submitted");Map<Integer,MahjongSuit>next=new LinkedHashMap<>(missingSuits);next.put(seat,Objects.requireNonNull(suit));return new OpeningSelections(exchanges,next);}
        public OpeningSelections timeoutExchange(int seat,List<Integer> hand){
            if(exchanges.containsKey(seat))return this; Map<Integer,List<Integer>> next=new LinkedHashMap<>(exchanges);
            List<Integer> choice=List.of(MahjongSuit.values()).stream().map(s->hand.stream().filter(s::contains).sorted().limit(3).toList()).filter(l->l.size()==3).min(Comparator.comparingInt(l->l.get(0)/10)).orElseThrow(()->new IllegalStateException("no legal exchange"));next.put(seat,choice);return new OpeningSelections(next,missingSuits);
        }
        public OpeningSelections timeoutMissingSuit(int seat,List<Integer> hand){
            if(missingSuits.containsKey(seat))return this;Map<Integer,MahjongSuit>next=new LinkedHashMap<>(missingSuits);MahjongSuit choice=List.of(MahjongSuit.values()).stream().min(Comparator.<MahjongSuit>comparingLong(s->hand.stream().filter(s::contains).count()).thenComparingInt(MahjongSuit::ordinal)).orElseThrow();next.put(seat,choice);return new OpeningSelections(exchanges,next);
        }
        public Map<String,Object> snapshot(){return Map.of("exchanges",exchanges,"missingSuits",missingSuits.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().name())));}
        public static OpeningSelections restore(Map<String,Object> snapshot){Map<Integer,List<Integer>>e=new LinkedHashMap<>();Object re=snapshot.get("exchanges");if(re instanceof Map<?,?>m)m.forEach((k,v)->e.put(Integer.parseInt(String.valueOf(k)),((List<?>)v).stream().map(x->((Number)x).intValue()).toList()));Map<Integer,MahjongSuit>s=new LinkedHashMap<>();Object rs=snapshot.get("missingSuits");if(rs instanceof Map<?,?>m)m.forEach((k,v)->s.put(Integer.parseInt(String.valueOf(k)),MahjongSuit.valueOf(String.valueOf(v))));return new OpeningSelections(e,s);}
        public Map<Integer,List<Integer>> applyExchange(List<Integer> seats,Map<Integer,List<Integer>> hands,int direction){Set<Integer>seatSet=new HashSet<>(seats);if(seatSet.size()!=seats.size()||!hands.keySet().equals(seatSet)||!exchanges.keySet().equals(seatSet))throw new IllegalStateException("seat/hand/exchange keys must match exactly");if(direction!=1&&direction!=-1)throw new IllegalArgumentException("direction must be clockwise or counter-clockwise");LinkedHashMap<Integer,List<Integer>>out=new LinkedHashMap<>();for(int i=0;i<seats.size();i++){int seat=seats.get(i),source=seats.get(Math.floorMod(i-direction,seats.size()));ArrayList<Integer>hand=new ArrayList<>(hands.get(seat));for(int tile:exchanges.get(seat))if(!hand.remove(Integer.valueOf(tile)))throw new IllegalStateException("submitted tile no longer in hand");hand.addAll(exchanges.get(source));out.put(seat,List.copyOf(hand));}return Map.copyOf(out);}
    }

    public enum Claim { PASS, CHOW, PONG, KONG, WIN }
    public record ClaimRequest(int seat, Claim claim) {}
    public enum ClaimContext { DISCARD, ADDED_KONG }
    public static List<ClaimRequest> arbitrate(List<ClaimRequest> requests,int sourceSeat,List<Integer> turnOrder,boolean multipleWins,ClaimContext context){if(context==ClaimContext.ADDED_KONG&&requests.stream().anyMatch(r->r.claim()!=Claim.WIN&&r.claim()!=Claim.PASS))throw new IllegalArgumentException("only win/pass allowed while robbing kong");return arbitrate(requests,sourceSeat,turnOrder,multipleWins);}
    public static List<ClaimRequest> arbitrate(List<ClaimRequest> requests, int discarder, List<Integer> turnOrder,
            boolean multipleWins) {
        if (requests.isEmpty()) return List.of();
        if(turnOrder.size()!=new HashSet<>(turnOrder).size()||!turnOrder.contains(discarder))throw new IllegalArgumentException("invalid turn order");Set<Integer>seen=new HashSet<>();for(ClaimRequest r:requests){if(r.claim()==null||!turnOrder.contains(r.seat())||r.seat()==discarder||!seen.add(r.seat()))throw new IllegalArgumentException("invalid/duplicate claim");}
        int best = requests.stream().mapToInt(r -> priority(r.claim())).max().orElse(0);
        List<ClaimRequest> top = requests.stream().filter(r -> priority(r.claim()) == best).toList();
        if (best == priority(Claim.WIN) && multipleWins) return top.stream().sorted(Comparator.comparingInt(r -> distance(discarder,r.seat(),turnOrder))).toList();
        return List.of(top.stream().min(Comparator.comparingInt(r -> distance(discarder,r.seat(),turnOrder))).orElseThrow());
    }
    private static int priority(Claim c) { return switch(c) { case WIN -> 4; case KONG, PONG -> 3; case CHOW -> 2; case PASS -> 1; }; }
    private static int distance(int from, int to, List<Integer> order) {
        int a=order.indexOf(from), b=order.indexOf(to); if(a<0||b<0)throw new IllegalArgumentException("seat absent from turn order");
        return Math.floorMod(b-a, order.size());
    }

    public enum KongType { CONCEALED, EXPOSED, ADDED }
    public record KongCapability(Set<KongType> types, boolean drawFromTail, boolean robAddedKong) {
        public KongCapability { types = Set.copyOf(types); }
        public KongResult apply(KongType type, List<Integer> wall) {
            if (!types.contains(type) || wall.isEmpty()) throw new IllegalStateException("kong is not available");
            ArrayList<Integer> next = new ArrayList<>(wall);
            int index = drawFromTail ? next.size()-1 : 0;
            return new KongResult(next.remove(index), List.copyOf(next), robAddedKong && type==KongType.ADDED);
        }
    }
    public record KongResult(int replacementTile, List<Integer> wall, boolean robbingWindow) {}

    public enum WinningForm { STANDARD, SEVEN_PAIRS, THIRTEEN_ORPHANS }
    public record WinPolicy(Set<WinningForm> forms, MahjongSuit forbiddenSuit) {
        public WinPolicy { forms=Set.copyOf(forms); }
        public boolean mayWin(List<Integer> tiles) {
            if (forbiddenSuit != null && tiles.stream().anyMatch(forbiddenSuit::contains)) return false;
            if (forms.contains(WinningForm.SEVEN_PAIRS) && sevenPairs(tiles)) return true;
            if (forms.contains(WinningForm.THIRTEEN_ORPHANS) && thirteenOrphans(tiles)) return true;
            return forms.contains(WinningForm.STANDARD) && new StandardMahjongWinDetector().isWinning(tiles, 0);
        }
    }
    private static boolean sevenPairs(List<Integer> tiles) { if(tiles.size()!=14)return false; Map<Integer,Long> c=new HashMap<>();tiles.forEach(t->c.merge(t,1L,Long::sum));return c.values().stream().allMatch(n->n%2==0)&&c.values().stream().mapToLong(Long::longValue).sum()==14; }
    private static boolean thirteenOrphans(List<Integer> tiles) { Set<Integer> required=Set.of(11,19,21,29,31,39,41,42,43,44,45,46,47);return tiles.size()==14&&tiles.containsAll(required)&&new HashSet<>(tiles).equals(required); }

    public enum WinSource { SELF_DRAW, DISCARD, ROB_KONG }
    public record Liability(int payer, int receiver, long amount, String reason) { public Liability { if(amount<=0||payer==receiver)throw new IllegalArgumentException("invalid liability"); } }
    public static List<Liability> liabilities(WinSource source, int winner, int discarder, List<Integer> activeSeats,
            long unit, Integer responsibleSeat) {
        if(source==null||unit<=0||activeSeats.size()!=new HashSet<>(activeSeats).size()||!activeSeats.contains(winner))throw new IllegalArgumentException("invalid liability request");
        if(responsibleSeat!=null&&(!activeSeats.contains(responsibleSeat)||responsibleSeat==winner))throw new IllegalArgumentException("invalid responsible seat");
        if(responsibleSeat==null&&source!=WinSource.SELF_DRAW&&(!activeSeats.contains(discarder)||discarder==winner))throw new IllegalArgumentException("invalid discarder");
        if(responsibleSeat!=null)return List.of(new Liability(responsibleSeat,winner,unit*(activeSeats.size()-1),"RESPONSIBILITY"));
        if(source==WinSource.SELF_DRAW)return activeSeats.stream().filter(s->s!=winner).map(s->new Liability(s,winner,unit,"SELF_DRAW")).toList();
        return List.of(new Liability(discarder,winner,unit*(activeSeats.size()-1),source.name()));
    }

    public record FanRule(String code, int fan, Set<String> excludes) { public FanRule { if(code==null||code.isBlank()||fan<0)throw new IllegalArgumentException("invalid fan rule");excludes=Set.copyOf(excludes); } }
    public record FanResult(int rawFan, int cappedFan, Set<String> applied) {}
    public static Map<String,FanRule> compileFanSchema(List<FanRule> schema){Map<String,FanRule>byCode=new LinkedHashMap<>();for(FanRule r:schema)if(byCode.put(r.code(),r)!=null)throw new IllegalArgumentException("duplicate fan: "+r.code());for(FanRule r:schema)for(String excluded:r.excludes())if(excluded.equals(r.code())||!byCode.containsKey(excluded))throw new IllegalArgumentException("invalid exclusion: "+r.code()+"->"+excluded);return Map.copyOf(byCode);}
    public static FanResult scoreFans(List<FanRule> schema, Set<String> matched, int cap) {
        if(cap<0)throw new IllegalArgumentException("invalid cap"); Map<String,FanRule> byCode=compileFanSchema(schema);if(!byCode.keySet().containsAll(matched))throw new IllegalArgumentException("unknown matched fan");
        LinkedHashSet<String> applied=new LinkedHashSet<>();
        matched.stream().map(byCode::get).filter(Objects::nonNull).sorted(Comparator.comparingInt(FanRule::fan).reversed().thenComparing(FanRule::code)).forEach(r->{if(applied.stream().noneMatch(a->r.excludes().contains(a)||byCode.get(a).excludes().contains(r.code())))applied.add(r.code());});
        int raw=applied.stream().mapToInt(c->byCode.get(c).fan()).sum(); return new FanResult(raw,Math.min(raw,cap),Set.copyOf(applied));
    }

    public record WallTiming(int initialSize, int remaining, boolean afterKong, boolean firstTurn) {
        public WallTiming { if(initialSize<=0||remaining<0||remaining>initialSize||firstTurn&&remaining==0||firstTurn&&afterKong)throw new IllegalArgumentException("invalid wall timing"); }
        public Set<String> winTags(boolean selfDraw) { LinkedHashSet<String> tags=new LinkedHashSet<>();if(remaining==0)tags.add(selfDraw?"HAI_DI_LAO":"HE_DI_LAO");if(afterKong)tags.add(selfDraw?"GANG_SHANG_HUA":"QIANG_GANG_HU");if(firstTurn)tags.add(selfDraw?"TIAN_HU":"DI_HU");return Set.copyOf(tags); }
    }

    public record PassRestrictions(Set<Integer> passedPongTiles, Set<Integer> passedWinTiles) {
        public PassRestrictions { passedPongTiles=Set.copyOf(passedPongTiles);passedWinTiles=Set.copyOf(passedWinTiles); }
        public static PassRestrictions empty(){return new PassRestrictions(Set.of(),Set.of());}
        public PassRestrictions pass(int tile, boolean couldPong, boolean couldWin){Set<Integer>p=new HashSet<>(passedPongTiles),w=new HashSet<>(passedWinTiles);if(couldPong)p.add(tile);if(couldWin)w.add(tile);return new PassRestrictions(p,w);}
        public boolean mayPong(int tile){return !passedPongTiles.contains(tile);} public boolean mayWin(int tile){return !passedWinTiles.contains(tile);}
        public PassRestrictions onOwnDraw(){return empty();}
        public Map<String,Object> snapshot(){return Map.of("pong",passedPongTiles.stream().sorted().toList(),"win",passedWinTiles.stream().sorted().toList());}
        public static PassRestrictions restore(Map<String,Object> m){return new PassRestrictions(intSet(m.get("pong")),intSet(m.get("win")));}
    }
    private static Set<Integer> intSet(Object raw){LinkedHashSet<Integer>s=new LinkedHashSet<>();if(raw instanceof Iterable<?>it)it.forEach(v->s.add(((Number)v).intValue()));return s;}

    public record DrawAudit(Set<Integer> flowerPigSeats,Set<Integer> readySeats,Set<Integer> notReadySeats,
            List<Liability> callReadyLiabilities, Map<Integer,Long> deltas, int nextDealer) {}
    public static DrawAudit auditDraw(Map<Integer,List<Integer>> hands, Map<Integer,MahjongSuit> missingSuits,
            long penalty, int dealer, DealerPolicy dealerPolicy) {
        return auditDraw(hands,missingSuits,penalty,dealer,dealerPolicy,1,hands.keySet(),0);
    }
    public static DrawAudit auditDraw(Map<Integer,List<Integer>> hands, Map<Integer,MahjongSuit> missingSuits,
            long penalty, int dealer, DealerPolicy dealerPolicy, int flowerPigThreshold) {
        return auditDraw(hands,missingSuits,penalty,dealer,dealerPolicy,flowerPigThreshold,hands.keySet(),0);
    }
    public static DrawAudit auditDraw(Map<Integer,List<Integer>> hands, Map<Integer,MahjongSuit> missingSuits,
            long penalty, int dealer, DealerPolicy dealerPolicy, int flowerPigThreshold,Set<Integer> readySeats,long notReadyPenalty) {
        if(flowerPigThreshold<1||penalty<0||notReadyPenalty<0||!hands.keySet().containsAll(readySeats))throw new IllegalArgumentException("invalid draw audit policy");
        LinkedHashSet<Integer> pigs=new LinkedHashSet<>();hands.forEach((seat,hand)->{MahjongSuit suit=missingSuits.get(seat);if(suit!=null&&hand.stream().filter(suit::contains).count()>=flowerPigThreshold)pigs.add(seat);});
        LinkedHashMap<Integer,Long>d=new LinkedHashMap<>();hands.keySet().forEach(s->d.put(s,0L));for(int pig:pigs)for(int other:hands.keySet())if(other!=pig){d.merge(pig,-penalty,Long::sum);d.merge(other,penalty,Long::sum);}
        LinkedHashSet<Integer>notReady=new LinkedHashSet<>(hands.keySet());notReady.removeAll(readySeats);ArrayList<Liability>callReady=new ArrayList<>();for(int payer:notReady)for(int receiver:readySeats)if(payer!=receiver&&notReadyPenalty>0){Liability l=new Liability(payer,receiver,notReadyPenalty,"CALL_READY");callReady.add(l);d.merge(payer,-notReadyPenalty,Long::sum);d.merge(receiver,notReadyPenalty,Long::sum);}
        return new DrawAudit(Set.copyOf(pigs),Set.copyOf(readySeats),Set.copyOf(notReady),List.copyOf(callReady),Map.copyOf(d),dealerPolicy.nextDealer(dealer,DealerOutcome.DRAW,new ArrayList<>(hands.keySet())));
    }

    public record WinEvent(int sequence, int winner, int payer, long amount) { public WinEvent { if(sequence<1||amount<=0||winner==payer)throw new IllegalArgumentException("invalid win event"); } }
    public static final class ContinuationLedger {
        private final boolean winnersRemainActive; private final List<Integer> seats;private final List<WinEvent> events=new ArrayList<>(); private final Set<Integer> inactive=new HashSet<>();private int currentSeat;private boolean finished;
        public ContinuationLedger(boolean winnersRemainActive,List<Integer> seats,int currentSeat){if(seats.size()<2||seats.size()!=new HashSet<>(seats).size()||!seats.contains(currentSeat))throw new IllegalArgumentException("invalid continuation seats");this.winnersRemainActive=winnersRemainActive;this.seats=List.copyOf(seats);this.currentSeat=currentSeat;}
        public WinEvent record(int winner,int payer,long amount){if(finished||inactive.contains(winner)||!seats.isEmpty()&&(!seats.contains(winner)||!seats.contains(payer)))throw new IllegalStateException("win unavailable");WinEvent e=new WinEvent(events.size()+1,winner,payer,amount);events.add(e);if(!winnersRemainActive)inactive.add(winner);if(!seats.isEmpty()){if(!winnersRemainActive&&inactive.size()>=seats.size()-1)finished=true;advance();}return e;}
        private void advance(){if(finished||seats.isEmpty())return;int i=seats.indexOf(currentSeat);for(int n=1;n<=seats.size();n++){int candidate=seats.get((i+n)%seats.size());if(!inactive.contains(candidate)){currentSeat=candidate;return;}}finished=true;}
        public void finishWhenWallEmpty(int remaining){if(remaining<0)throw new IllegalArgumentException("invalid wall");if(remaining==0)finished=true;}
        public boolean mayOperate(int seat){return !finished&&(seats.isEmpty()||seats.contains(seat))&&!inactive.contains(seat);} public List<WinEvent> events(){return List.copyOf(events);}public int currentSeat(){return currentSeat;}public boolean finished(){return finished;}
        public Map<String,Object> snapshot(){return Map.of("winnersRemainActive",winnersRemainActive,"seats",seats,"events",events.stream().map(e->Map.of("sequence",e.sequence(),"winner",e.winner(),"payer",e.payer(),"amount",e.amount())).toList(),"inactive",inactive.stream().sorted().toList(),"currentSeat",currentSeat,"finished",finished);}
        public static ContinuationLedger restore(Map<String,Object>m){ContinuationLedger l=new ContinuationLedger((Boolean)m.get("winnersRemainActive"),((List<?>)m.get("seats")).stream().map(v->((Number)v).intValue()).toList(),((Number)m.get("currentSeat")).intValue());int expected=1;for(Object raw:(List<?>)m.get("events")){Map<?,?>e=(Map<?,?>)raw;WinEvent event=new WinEvent(((Number)e.get("sequence")).intValue(),((Number)e.get("winner")).intValue(),((Number)e.get("payer")).intValue(),((Number)e.get("amount")).longValue());if(event.sequence()!=expected++||!l.seats.isEmpty()&&(!l.seats.contains(event.winner())||!l.seats.contains(event.payer())))throw new IllegalArgumentException("invalid continuation event snapshot");l.events.add(event);}l.inactive.addAll(((List<?>)m.get("inactive")).stream().map(v->((Number)v).intValue()).toList());l.finished=(Boolean)m.get("finished");Set<Integer>winners=l.events.stream().map(WinEvent::winner).collect(java.util.stream.Collectors.toSet());if(!l.seats.containsAll(l.inactive)||l.winnersRemainActive&&!l.inactive.isEmpty()||!l.winnersRemainActive&&!l.inactive.equals(winners)||!l.finished&&!l.seats.isEmpty()&&l.inactive.contains(l.currentSeat)||!l.winnersRemainActive&&!l.seats.isEmpty()&&l.finished!=(l.inactive.size()>=l.seats.size()-1))throw new IllegalArgumentException("inconsistent continuation snapshot");return l;}
    }

    public record SettlementLine(int payer,int receiver,long points,String category,String source) { public SettlementLine { if(points<=0||payer==receiver||category==null||category.isBlank()||source==null||source.isBlank())throw new IllegalArgumentException("invalid settlement line"); } }
    public record SettlementDetail(List<SettlementLine> lines, Map<Integer,Long> totals) {}
    public static SettlementDetail settle(List<Integer> seats,List<SettlementLine> lines){if(seats.size()!=new HashSet<>(seats).size())throw new IllegalArgumentException("duplicate settlement seat");LinkedHashMap<Integer,Long>totals=new LinkedHashMap<>();seats.forEach(s->totals.put(s,0L));for(SettlementLine l:lines){if(!totals.containsKey(l.payer())||!totals.containsKey(l.receiver()))throw new IllegalArgumentException("unknown settlement seat");totals.merge(l.payer(),-l.points(),Long::sum);totals.merge(l.receiver(),l.points(),Long::sum);}if(totals.values().stream().mapToLong(Long::longValue).sum()!=0)throw new IllegalStateException("settlement is not conserved");return new SettlementDetail(List.copyOf(lines),Map.copyOf(totals));}
}
