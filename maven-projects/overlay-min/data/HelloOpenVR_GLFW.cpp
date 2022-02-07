/**** Minimal OpenVR example : outputs textures to the headset, a different color in each eye.  No head tracking or input anywhere.  Bare minimum. ****/


/// header only extension loader and object oriented bindings -- ref : https://github.com/VirtuosoChris/glhpp
#define GL_ALT_GL_API 1
#define GL_ALT_GLES_API 2

#include <glalt/gl4.5.h>	// opengl api
#include <glalt/glext.h>
#include <opengl.hpp>		// object oriented bindings

#include <GLFW/glfw3.h>
#include <openvr.h>

#include <iostream>
#include <string>

// #define LOG_FORMATS

static void error_callback(int error, const char* description)
{
	std::cerr<< "Error " << error << " : " << description << std::endl;
}


static void key_callback(GLFWwindow* window, int key, int scancode, int action, int mods)
{
	if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS)
		glfwSetWindowShouldClose(window, GLFW_TRUE);
}


std::ostream& operator<<(std::ostream& str, const GLFWvidmode& vidmode)
{
	str << "\n{ // GLFWvidmode \n";
	str << "\twidth = " << vidmode.width <<'\n';
	str << "\theight = " << vidmode.height <<'\n';
	str << "\tredBits = " << vidmode.redBits<<'\n';
	str << "\tgreenBits = "<< vidmode.greenBits<<'\n';
	str << "\tblueBits = "<< vidmode.blueBits<<'\n';
	str << "\trefreshRate = "<<vidmode.refreshRate<<'\n';
	str << "\n}\n" << std::endl;

	return str;
}


std::string GetTrackedDeviceString( vr::IVRSystem *pHmd, vr::TrackedDeviceIndex_t unDevice, vr::TrackedDeviceProperty prop, vr::TrackedPropertyError *peError = NULL )
{
	uint32_t unRequiredBufferLen = pHmd->GetStringTrackedDeviceProperty( unDevice, prop, NULL, 0, peError );
	if( unRequiredBufferLen == 0 )
		return "";

	char *pchBuffer = new char[ unRequiredBufferLen ];
	unRequiredBufferLen = pHmd->GetStringTrackedDeviceProperty( unDevice, prop, pchBuffer, unRequiredBufferLen, peError );
	std::string sResult = pchBuffer;
	delete [] pchBuffer;
	return sResult;
}


struct OpenVRApplication
{
	vr::IVRSystem* hmd;
	uint32_t rtWidth;
	uint32_t rtHeight;

	OpenVRApplication() : 
		hmd(NULL),
		rtWidth(0), rtHeight(0)
	{
		if (!hmdIsPresent())
		{
			throw std::runtime_error("Error : HMD not detected on the system");
		}

		if (!vr::VR_IsRuntimeInstalled())
		{
			throw std::runtime_error("Error : OpenVR Runtime not detected on the system");
		}

		initVR();

		if (!vr::VRCompositor())
		{
			throw std::runtime_error("Unable to initialize VR compositor!\n ");
		}

		hmd->GetRecommendedRenderTargetSize(&rtWidth, &rtHeight);

		std::clog<<"Initialized HMD with suggested render target size : " << rtWidth << "x" << rtHeight << std::endl;
	}

	/// returns if the system believes there is an HMD present without initializing all of OpenVR
	inline static bool hmdIsPresent()
	{
		return vr::VR_IsHmdPresent();
	}

	virtual ~OpenVRApplication()
	{
		if (hmd)
		{
			vr::VR_Shutdown();
			hmd = NULL;
		}
	}

	void submitFramesOpenGL(GLint leftEyeTex, GLint rightEyeTex, bool linear = false)
	{
		if (!hmd)
		{
			throw std::runtime_error("Error : presenting frames when VR system handle is NULL");
		}

		vr::TrackedDevicePose_t trackedDevicePose[vr::k_unMaxTrackedDeviceCount];
		vr::VRCompositor()->WaitGetPoses(trackedDevicePose, vr::k_unMaxTrackedDeviceCount, nullptr, 0);

		///\todo the documentation on this is completely unclear.  I have no idea which one is correct...
		/// seems to imply that we always want Gamma in opengl because linear is for framebuffers that have been
		/// processed by DXGI...
		vr::EColorSpace colorSpace = linear ? vr::ColorSpace_Linear : vr::ColorSpace_Gamma;
		
		vr::Texture_t leftEyeTexture = {(void*)leftEyeTex, vr::API_OpenGL, colorSpace};
		vr::Texture_t rightEyeTexture = {(void*)rightEyeTex, vr::API_OpenGL, colorSpace};
		
		vr::VRCompositor()->Submit(vr::Eye_Left, &leftEyeTexture);
		vr::VRCompositor()->Submit(vr::Eye_Right, &rightEyeTexture);

		vr::VRCompositor()->PostPresentHandoff();
	}

	void handleVRError(vr::EVRInitError err)
	{
		throw std::runtime_error(vr::VR_GetVRInitErrorAsEnglishDescription(err));
	}

	void initVR()
	{
		vr::EVRInitError err = vr::VRInitError_None;
		hmd = vr::VR_Init(&err, vr::VRApplication_Scene);

		if (err != vr::VRInitError_None)
		{
			handleVRError(err);
		}

		std::clog << GetTrackedDeviceString( hmd, vr::k_unTrackedDeviceIndex_Hmd, vr::Prop_TrackingSystemName_String ) << std::endl;
		std::clog << GetTrackedDeviceString( hmd, vr::k_unTrackedDeviceIndex_Hmd, vr::Prop_SerialNumber_String ) << std::endl;

	}
};


/************ Test application code **************/

struct RenderTarget
{
    gl::Framebuffer fbo;
    
    unsigned int frameWidth; ///< one half the allocated render target width, since we are using side by side stereo
    unsigned int frameHeight;
    unsigned int multisamples;
    RenderTarget(unsigned int width, unsigned int height, unsigned int samples) :
    frameWidth(width), frameHeight(height), multisamples(samples)
    {
    }
};


struct BasicRenderTarget : public RenderTarget
{
    gl::Renderbuffer depthTex;

    void prime(GLuint tex)
    {
        fbo.Bind(GL_FRAMEBUFFER);

#if GL_ALT_API_NAME == GL_ALT_GLES_API
        if (multisamples > 1)
        {
            glFramebufferTexture2DMultisampleEXT(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                                                 tex, 0, multisamples);
        }
        else
#endif
        {
            fbo.Texture(GL_COLOR_ATTACHMENT0, tex, 0);
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }


    BasicRenderTarget(int multisamples, unsigned int width, unsigned int height) :
            RenderTarget(width, height, multisamples)
    {
        const GLenum depthFormat = GL_DEPTH_COMPONENT24;

#if GL_ALT_API_NAME == GL_ALT_GLES_API
        if (multisamples > 1)
        {
            std::clog<<"Side by side with multisamples : "<<multisamples<<std::endl;
            depthTex.Bind();
            glRenderbufferStorageMultisampleEXT(GL_RENDERBUFFER, multisamples, depthFormat, width, height);
        }
        else
#endif
        {
            std::clog<<"Side by side without multisampling"<<std::endl;
            depthTex.Storage(depthFormat, width, height);
        }

        fbo.Renderbuffer(GL_DEPTH_ATTACHMENT, depthTex); ///\todo was depthTex

        static const GLenum draw_buffers[] = {GL_COLOR_ATTACHMENT0};

        glViewport(0, 0, frameWidth, frameHeight);
    }
};


int main(void)
{
	try
	{
		GLFWwindow* window;

		glfwSetErrorCallback(error_callback);

		if (!glfwInit())
		{
			exit(EXIT_FAILURE);
		}

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);

		OpenVRApplication vrApp;

		window = glfwCreateWindow(vrApp.rtWidth, vrApp.rtHeight, "Hello GLFW", NULL, NULL);

		if (!window)
		{
			glfwTerminate();
			exit(EXIT_FAILURE);
		}

	#ifdef LOG_FORMATS

		int count;
		const GLFWvidmode* modes = glfwGetVideoModes(glfwGetPrimaryMonitor(), &count);

		for (int i = 0; i < count; i++)
		{
			std::clog << modes[i]<<std::endl;	
		}

	#endif

		glfwSetKeyCallback(window, key_callback);

		glfwMakeContextCurrent(window);

		glfwSwapInterval(0);

		glClearColor (0.0f, 0.0f, 1.0f, 1.0f);

		{
			gl::Texture leftEyeTexture;
			leftEyeTexture.Storage2D(1, GL_RGBA8, vrApp.rtWidth, vrApp.rtHeight);

			gl::Texture rightEyeTexture;
			rightEyeTexture.Storage2D(1, GL_RGBA8, vrApp.rtWidth, vrApp.rtHeight);

			BasicRenderTarget leftRT(1, vrApp.rtWidth, vrApp.rtHeight);
			BasicRenderTarget rightRT(1, vrApp.rtWidth, vrApp.rtHeight);

			leftRT.prime(leftEyeTexture.name);
			rightRT.prime(rightEyeTexture.name);

			glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
			leftRT.fbo.Bind(GL_FRAMEBUFFER);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
			{
				throw std::runtime_error("left rt incomplete");
			}

			glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
			rightRT.fbo.Bind(GL_FRAMEBUFFER);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
			{
				throw std::runtime_error("right rt incomplete");
			}

			glBindFramebuffer(GL_FRAMEBUFFER, 0);

			glClearColor (0.0f, 0.0f, 1.0f, 1.0f);

			while (!glfwWindowShouldClose(window))
			{
				int width, height;
				glfwGetFramebufferSize(window, &width, &height);

				glViewport(0, 0, width, height);
				glClear(GL_COLOR_BUFFER_BIT);

				vrApp.submitFramesOpenGL(leftEyeTexture.name, rightEyeTexture.name);

				glfwSwapBuffers(window);
				glfwPollEvents();
			}
		}
		
		vr::VR_Shutdown(); ///\todo if I don't include this here, and just let the destructor handle shutting down VR, the process never terminates correctly, and breaks VR until I reboot.

		glfwDestroyWindow(window);
		glfwTerminate();
	}
	catch (const std::runtime_error& err)
	{
		std::cerr<< err.what() <<std::endl;
		
#ifdef _WIN32
		system("pause");
#endif

		exit(EXIT_FAILURE);
	}


	std::clog <<" End" << std::endl;
	exit(EXIT_SUCCESS);
	return 0;
}