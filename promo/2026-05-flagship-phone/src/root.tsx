import {Composition, Still} from 'remotion';
import {ItchCover, ItchScreenshot, TrainIqPromo} from './trainiq-promo';

export const Root = () => {
  return (
    <>
      <Composition
        id="TrainIqFlagshipPromo"
        component={TrainIqPromo}
        durationInFrames={810}
        fps={30}
        width={1080}
        height={1920}
      />
      <Still id="ItchCover" component={ItchCover} width={630} height={500} />
      {Array.from({length: 6}).map((_, index) => (
        <Still
          key={index}
          id={`ItchScreenshot${index + 1}`}
          component={ItchScreenshot}
          width={1600}
          height={900}
          defaultProps={{index}}
        />
      ))}
    </>
  );
};
