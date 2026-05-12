import {Audio} from '@remotion/media';
import {
  AbsoluteFill,
  Easing,
  Img,
  interpolate,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';

export type PromoScene = {
  image: string;
  kicker: string;
  title: string;
  body: string;
};

export const scenes: PromoScene[] = [
  {
    image: 'screenshots/01-home-dashboard.png',
    kicker: 'TrainIQ',
    title: 'Data wordt coaching',
    body: 'Gezondheid, training en voeding in een rustige cockpit.',
  },
  {
    image: 'screenshots/02-training-plan.png',
    kicker: 'Training',
    title: 'Je plan staat klaar',
    body: 'Start routines, volg volume en log sets zonder gedoe.',
  },
  {
    image: 'screenshots/07-active-workout.png',
    kicker: 'Actieve sessie',
    title: 'Sterker trainen',
    body: 'Gewicht, reps, RPE en rust blijven direct zichtbaar.',
  },
  {
    image: 'screenshots/03-nutrition.png',
    kicker: 'Voeding',
    title: 'Intake in beeld',
    body: 'Maaltijden, producten en AI-resultaten samen in een flow.',
  },
  {
    image: 'screenshots/05-coach.png',
    kicker: 'Coach',
    title: 'Inzichten die sturen',
    body: 'Weekrapporten en doeladvies op basis van je echte context.',
  },
  {
    image: 'screenshots/06-settings-health-connect.png',
    kicker: 'Health Connect',
    title: 'Bijna onzichtbaar',
    body: 'Automatisch verzamelen. Proactief advies wanneer het telt.',
  },
];

const colors = {
  ink: '#20242f',
  muted: '#596375',
  primary: '#53669f',
  green: '#1b6f55',
  paper: '#f4f8fb',
};

const introFrames = 84;
const sceneFrames = 102;
const transitionFrames = 18;
const outroStart = introFrames + scenes.length * sceneFrames + 30;
const phoneEnterEnd = introFrames + 18;
const screenSlideDistance = 430;

const clamp = {
  extrapolateLeft: 'clamp' as const,
  extrapolateRight: 'clamp' as const,
};

export const TrainIqPromo = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const intro = spring({frame, fps, config: {damping: 24, stiffness: 82}});
  const sceneTiming = getSceneTiming(frame);
  const finalProgress = interpolate(frame, [outroStart, outroStart + 42], [0, 1], {
    ...clamp,
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const phoneExit = interpolate(frame, [outroStart - 34, outroStart], [1, 0], {
    ...clamp,
    easing: Easing.bezier(0.7, 0, 0.84, 0),
  });
  const showScenes = frame < outroStart;
  const phoneEnter = interpolate(frame, [0, phoneEnterEnd], [0, 1], {
    ...clamp,
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const phoneOpacity = showScenes ? phoneEnter * phoneExit : 0;

  return (
    <AbsoluteFill style={{backgroundColor: '#edf4f7', fontFamily: 'Inter, Arial, sans-serif', overflow: 'hidden'}}>
      <Backdrop />
      <Brand />
      <HeroCopy intro={intro} />
      <div
        style={{
          ...styles.phoneStage,
          opacity: phoneOpacity,
          transform: `translate(-50%, ${interpolate(phoneEnter, [0, 1], [360, 0], clamp)}px) scale(${0.86 + phoneEnter * 0.14})`,
        }}
      >
        <PhoneFrame timing={sceneTiming} />
      </div>
      <Caption scene={scenes[sceneTiming.captionIndex]} localFrame={sceneTiming.localFrame} visible={phoneOpacity} />
      <FinalCard progress={finalProgress} />
      <Audio src={staticFile('audio/trainiq-pulse.wav')} volume={(f) => interpolate(f, [0, fps, 25 * fps, 27 * fps], [0, 0.34, 0.34, 0], clamp)} />
    </AbsoluteFill>
  );
};

type SceneTiming = {
  captionIndex: number;
  localFrame: number;
  screenLayers: Array<{
    sceneIndex: number;
    opacity: number;
    x: number;
  }>;
};

const getSceneTiming = (frame: number): SceneTiming => {
  const cursor = frame - introFrames;
  const settledIndex = Math.max(0, Math.min(scenes.length - 1, Math.floor(Math.max(0, cursor) / sceneFrames)));
  const localFrame = Math.max(0, Math.min(sceneFrames - 1, cursor - settledIndex * sceneFrames));

  if (frame < introFrames) {
    return {
      captionIndex: 0,
      localFrame: 0,
      screenLayers: [{sceneIndex: 0, opacity: 1, x: 0}],
    };
  }

  for (let nextIndex = 1; nextIndex < scenes.length; nextIndex++) {
    const cutFrame = introFrames + nextIndex * sceneFrames;
    const transitionStart = cutFrame - transitionFrames;
    const transitionEnd = cutFrame + transitionFrames;

    if (frame >= transitionStart && frame < transitionEnd) {
      const progress = interpolate(frame, [transitionStart, transitionEnd], [0, 1], {
        ...clamp,
        easing: Easing.bezier(0.4, 0, 0.2, 1),
      });

      return {
        captionIndex: progress < 0.55 ? nextIndex - 1 : nextIndex,
        localFrame: progress < 0.55 ? sceneFrames - transitionFrames : transitionFrames,
        screenLayers: [
          {sceneIndex: nextIndex - 1, opacity: 1, x: interpolate(progress, [0, 1], [0, -screenSlideDistance], clamp)},
          {sceneIndex: nextIndex, opacity: 1, x: interpolate(progress, [0, 1], [screenSlideDistance, 0], clamp)},
        ],
      };
    }
  }

  return {
    captionIndex: settledIndex,
    localFrame,
    screenLayers: [{sceneIndex: settledIndex, opacity: 1, x: 0}],
  };
};

const Backdrop = () => {
  const frame = useCurrentFrame();
  return (
    <>
      <Img
        src={staticFile('generated/premium-health-backdrop.png')}
        style={{
          position: 'absolute',
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          opacity: 0.68,
          transform: `scale(${1.04 + frame * 0.00009})`,
        }}
      />
      <div style={styles.veil} />
    </>
  );
};

const Brand = () => (
  <div style={styles.brand}>
    <div style={styles.mark}>TIQ</div>
    <div>
      <div style={styles.brandTitle}>TrainIQ</div>
      <div style={styles.brandSub}>AI-native health coaching</div>
    </div>
  </div>
);

const HeroCopy = ({intro}: {intro: number}) => {
  const frame = useCurrentFrame();
  return (
    <div
      style={{
        ...styles.heroCopy,
        opacity: interpolate(frame, [0, 22, 58, 82], [0, 1, 1, 0], clamp),
        transform: `translateY(${interpolate(intro, [0, 1], [32, 0])}px)`,
      }}
    >
      <div style={styles.kicker}>Health Connect naar persoonlijke actie</div>
      <div style={styles.heroTitle}>Je lichaam praat al. TrainIQ vertaalt.</div>
    </div>
  );
};

const PhoneFrame = ({timing}: {timing: SceneTiming}) => {
  return (
    <div style={styles.phone}>
      <div style={styles.speaker} />
      <div style={styles.screen}>
        {timing.screenLayers.map((layer) => {
          const scene = scenes[layer.sceneIndex];
          return (
            <Img
              key={scene.image}
              src={staticFile(scene.image)}
              style={{
                position: 'absolute',
                inset: 0,
                width: '100%',
                height: '100%',
                objectFit: 'cover',
                objectPosition: 'center top',
                opacity: layer.opacity,
                transform: `translateX(${layer.x}px)`,
              }}
            />
          );
        })}
      </div>
      <div style={styles.phoneShine} />
    </div>
  );
};

const Caption = ({scene, localFrame, visible}: {scene: PromoScene; localFrame: number; visible: number}) => {
  const opacity = visible * interpolate(localFrame, [8, 24, sceneFrames - 28, sceneFrames - 12], [0, 1, 1, 0], clamp);
  return (
    <div
      style={{
        ...styles.caption,
        opacity,
        transform: `translateX(-50%) translateY(${interpolate(opacity, [0, 1], [24, 0], clamp)}px)`,
      }}
    >
      <div style={styles.kicker}>{scene.kicker}</div>
      <div style={styles.sceneTitle}>{scene.title}</div>
      <div style={styles.sceneBody}>{scene.body}</div>
    </div>
  );
};

const FinalCard = ({progress}: {progress: number}) => {
  return (
    <div
      style={{
        ...styles.final,
        opacity: progress,
        transform: `translate(-50%, ${interpolate(progress, [0, 1], [46, 0], clamp)}px) scale(${0.96 + progress * 0.04})`,
      }}
    >
      <div style={styles.finalMark}>TIQ</div>
      <div style={styles.finalTitle}>TrainIQ</div>
      <div style={styles.finalBody}>Passieve gezondheidsdata. Actieve coaching.</div>
    </div>
  );
};

export const ItchCover = () => {
  return (
    <AbsoluteFill style={{backgroundColor: '#eef5f7', fontFamily: 'Inter, Arial, sans-serif', overflow: 'hidden'}}>
      <Backdrop />
      <div style={{...styles.itchTextBlock, left: 34, top: 54, width: 248}}>
        <div style={{...styles.itchEyebrow, fontSize: 17}}>TrainIQ</div>
        <div style={{...styles.itchTitle, fontSize: 49}}>Data wordt coaching</div>
        <div style={{...styles.itchBody, fontSize: 20}}>AI-powered strength coaching from health and workout data.</div>
      </div>
      <div style={{...styles.itchPhone, right: 34, top: 44, width: 210, height: 466}}>
        <PhoneStill image="screenshots/01-home-dashboard.png" />
      </div>
    </AbsoluteFill>
  );
};

export const ItchScreenshot = ({index = 0}: {index?: number}) => {
  const scene = scenes[index] ?? scenes[0];
  const phoneLeft = index % 2 === 0;
  return (
    <AbsoluteFill style={{backgroundColor: '#eef5f7', fontFamily: 'Inter, Arial, sans-serif', overflow: 'hidden'}}>
      <Backdrop />
      <div style={{...styles.galleryPhone, left: phoneLeft ? 168 : undefined, right: phoneLeft ? undefined : 168}}>
        <PhoneStill image={scene.image} />
      </div>
      <div style={{...styles.itchTextBlock, left: phoneLeft ? 890 : 120, top: 210, width: 510}}>
        <div style={styles.itchEyebrow}>{scene.kicker}</div>
        <div style={styles.itchTitle}>{scene.title}</div>
        <div style={styles.itchBody}>{scene.body}</div>
      </div>
    </AbsoluteFill>
  );
};

const PhoneStill = ({image}: {image: string}) => (
  <div style={{...styles.phone, borderRadius: 48, padding: 12}}>
    <div style={{...styles.speaker, top: 18, left: '50%', transform: 'translateX(-50%)', width: 72, height: 12}} />
    <div style={{...styles.screen, inset: 12, borderRadius: 38}}>
      <Img src={staticFile(image)} style={{width: '100%', height: '100%', objectFit: 'cover', objectPosition: 'center top'}} />
    </div>
    <div style={{...styles.phoneShine, inset: 12, borderRadius: 38}} />
  </div>
);

const styles: Record<string, React.CSSProperties> = {
  veil: {
    position: 'absolute',
    inset: 0,
    background:
      'radial-gradient(circle at 50% 36%, rgba(255,255,255,0.8), rgba(255,255,255,0.32) 34%, rgba(229,239,242,0.9) 78%), linear-gradient(180deg, rgba(255,255,255,0.6), rgba(225,238,242,0.92))',
  },
  brand: {
    position: 'absolute',
    top: 58,
    left: 64,
    display: 'flex',
    gap: 18,
    alignItems: 'center',
    color: colors.ink,
  },
  mark: {
    width: 62,
    height: 62,
    borderRadius: 18,
    display: 'grid',
    placeItems: 'center',
    background: 'rgba(255,255,255,0.72)',
    border: '1px solid rgba(83,102,159,0.2)',
    color: colors.green,
    fontWeight: 900,
    fontSize: 22,
    letterSpacing: 0,
  },
  brandTitle: {fontSize: 34, fontWeight: 850, letterSpacing: 0},
  brandSub: {fontSize: 20, color: colors.muted, marginTop: 2},
  heroCopy: {
    position: 'absolute',
    left: 74,
    right: 74,
    top: 1190,
    color: colors.ink,
  },
  kicker: {
    textTransform: 'uppercase',
    color: colors.green,
    fontSize: 23,
    fontWeight: 800,
    letterSpacing: 1.2,
    marginBottom: 12,
  },
  heroTitle: {
    fontSize: 72,
    lineHeight: 0.96,
    fontWeight: 900,
    letterSpacing: 0,
    maxWidth: 880,
  },
  phoneStage: {
    position: 'absolute',
    top: 158,
    left: '50%',
    width: 462,
    height: 1026,
    filter: 'drop-shadow(0 38px 72px rgba(21,35,55,0.28))',
  },
  phone: {
    position: 'absolute',
    inset: 0,
    borderRadius: 64,
    background: 'linear-gradient(145deg, #171b22, #3b424d)',
    padding: 16,
    boxShadow: 'inset 0 0 0 2px rgba(255,255,255,0.22)',
  },
  speaker: {
    position: 'absolute',
    top: 25,
    left: 180,
    width: 102,
    height: 17,
    borderRadius: 999,
    background: '#0d1117',
    zIndex: 4,
    opacity: 0.9,
  },
  screen: {
    position: 'absolute',
    inset: 16,
    borderRadius: 50,
    overflow: 'hidden',
    background: colors.paper,
  },
  phoneShine: {
    position: 'absolute',
    inset: 16,
    borderRadius: 50,
    background: 'linear-gradient(116deg, rgba(255,255,255,0.2), rgba(255,255,255,0) 34%)',
    pointerEvents: 'none',
  },
  caption: {
    position: 'absolute',
    left: '50%',
    top: 1304,
    width: 790,
    minHeight: 250,
    color: colors.ink,
    padding: '34px 38px 36px',
    borderRadius: 34,
    background: 'rgba(255,255,255,0.76)',
    border: '1px solid rgba(83,102,159,0.16)',
    boxShadow: '0 30px 70px rgba(34,50,70,0.12)',
    backdropFilter: 'blur(18px)',
  },
  sceneTitle: {
    fontSize: 54,
    lineHeight: 1,
    fontWeight: 900,
    letterSpacing: 0,
  },
  sceneBody: {
    marginTop: 18,
    fontSize: 28,
    lineHeight: 1.2,
    fontWeight: 650,
    color: colors.muted,
  },
  final: {
    position: 'absolute',
    left: '50%',
    width: 850,
    top: 676,
    borderRadius: 44,
    padding: '58px 50px 62px',
    textAlign: 'center',
    color: colors.ink,
    background: 'rgba(255,255,255,0.78)',
    border: '1px solid rgba(83,102,159,0.18)',
    boxShadow: '0 42px 92px rgba(34,50,70,0.16)',
  },
  finalMark: {
    margin: '0 auto 26px',
    width: 112,
    height: 112,
    borderRadius: 32,
    display: 'grid',
    placeItems: 'center',
    background: '#eef8f4',
    color: colors.green,
    fontSize: 36,
    fontWeight: 900,
  },
  finalTitle: {
    fontSize: 88,
    fontWeight: 950,
    letterSpacing: 0,
  },
  finalBody: {
    marginTop: 18,
    color: colors.muted,
    fontSize: 34,
    fontWeight: 720,
  },
  itchPhone: {
    position: 'absolute',
    filter: 'drop-shadow(0 24px 42px rgba(21,35,55,0.26))',
  },
  galleryPhone: {
    position: 'absolute',
    top: 78,
    width: 352,
    height: 782,
    filter: 'drop-shadow(0 30px 60px rgba(21,35,55,0.24))',
  },
  itchTextBlock: {
    position: 'absolute',
    color: colors.ink,
  },
  itchEyebrow: {
    textTransform: 'uppercase',
    color: colors.green,
    fontSize: 28,
    fontWeight: 850,
    letterSpacing: 1.4,
    marginBottom: 20,
  },
  itchTitle: {
    fontSize: 82,
    lineHeight: 0.96,
    fontWeight: 950,
    letterSpacing: 0,
  },
  itchBody: {
    marginTop: 24,
    fontSize: 33,
    lineHeight: 1.18,
    fontWeight: 650,
    color: colors.muted,
  },
};
